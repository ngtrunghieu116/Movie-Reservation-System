package com.moviebooking.service.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moviebooking.dto.req.ChangePasswordRequest;
import com.moviebooking.dto.req.ForgotPasswordRequest;
import com.moviebooking.dto.req.GoogleLoginRequest;
import com.moviebooking.dto.req.LoginRequest;
import com.moviebooking.dto.req.RegisterRequest;
import com.moviebooking.dto.req.ResetPasswordRequest;
import com.moviebooking.dto.res.AuthResponse;
import com.moviebooking.model.PasswordResetToken;
import com.moviebooking.model.User;
import com.moviebooking.model.enums.Gender;
import com.moviebooking.model.enums.Role;
import com.moviebooking.repository.PasswordResetTokenRepository;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.model.EmailVerificationToken;
import com.moviebooking.repository.EmailVerificationTokenRepository;
import com.moviebooking.exception.EmailNotVerifiedException;
import com.moviebooking.security.CustomUserDetails;
import com.moviebooking.security.JwtService;
import com.moviebooking.service.email.EmailService;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Collections.singletonList;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Override
    @Transactional // Đảm bảo nếu lỗi giữa chừng thì DB sẽ rollback (quay lại trạng thái cũ)
    public String register(RegisterRequest request) {

        // 1. Kiểm tra Email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng. Vui lòng chọn email khác!");
        }
        // 2. Kiểm tra Số điện thoại đã tồn tại chưa
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng. Vui lòng chọn số khác!");
        }
        // 3. Khởi tạo Entity User từ thông tin DTO gửi lên
        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                // MÃ HÓA MẬT KHẨU TRƯỚC KHI LƯU
                .password(passwordEncoder.encode(request.getPassword()))
                // Cài đặt mặc định cho User mới
                .role(Role.USER)
                .emailVerified(false)
                .build();
        // 4. Lưu vào Database
        userRepository.save(newUser);

        // 5. Tạo token xác thực email và gửi
        String token = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = EmailVerificationToken.builder()
                .token(token)
                .user(newUser)
                .expiryDate(java.time.LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(emailToken);

        String verifyLink = "http://localhost:8080/api/auth/verify-email?token=" + token;

        try {
            emailService.sendEmail(
                    newUser.getEmail(),
                    "Xác thực tài khoản Cinemind",
                    "Chào bạn,\n\nVui lòng click vào link sau để xác thực email của bạn:\n" + verifyLink,
                    false);
        } catch (Exception e) {
            System.err.println("Gửi email thất bại, link xác thực là: " + verifyLink);
        }

        return "Đăng ký tài khoản thành công! Vui lòng kiểm tra email để xác thực.";
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Spring Security kiểm tra tài khoản & mật khẩu
        // Nếu sai mật khẩu, hàm này tự ném lỗi BadCredentialsException (trả về 401
        // Unauthorized)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), // Ta dùng email thay cho username
                        request.getPassword()));
        // 2. Nếu mật khẩu đúng, lấy thông tin User từ Database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        // Kiểm tra xác thực email
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException("Email chưa được xác thực!");
        }

        // 3. Sinh Token (Stateless)
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        // 4. Chỉ trả về Token cho Frontend qua DTO, KHÔNG LƯU VÀO DATABASE
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(user.getRole().name())
                .message("Đăng nhập thành công!")
                .build();
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu không khớp!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new RuntimeException("ID Token Google không hợp lệ!");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            if (firstName == null)
                firstName = name;
            if (lastName == null)
                lastName = "";

            // Kiểm tra user có tồn tại chưa
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                // Tạo user mới
                user = User.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .phone("") // Không có từ google
                        .dateOfBirth(java.time.LocalDate.of(2000, 1, 1)) // Default
                        .gender(Gender.OTHER) // Default
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                        .role(Role.USER)
                        .emailVerified(true)
                        .build();
                userRepository.save(user);
            }

            // Sinh Token
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .role(user.getRole().name())
                    .message("Đăng nhập Google thành công!")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác thực Google: " + e.getMessage());
        }
    }

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Không tiết lộ email có tồn tại hay không
        if (user == null) {
            return;
        }

        // Disable token cũ
        passwordResetTokenRepository
                .findByUserAndUsedFalse(user)
                .forEach(token -> token.setUsed(true));

        String token = java.util.UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        String resetLink = resetPasswordUrl + "?token=" + token;

        emailService.sendEmail(
                user.getEmail(),
                "Reset Password",
                buildResetPasswordEmail(resetLink),
                false);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu không khớp!");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ."));

        if (resetToken.getUsed()) {
            throw new RuntimeException("Token đã được sử dụng.");
        }

        if (resetToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn.");
        }

        User userProxy = resetToken.getUser();
        User user = userRepository.findById(userProxy.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        resetToken.setUsed(true);

        passwordResetTokenRepository.save(resetToken);

    }

    private String buildResetPasswordEmail(String resetLink) {

        return """
                Hello,

                We received a request to reset your password.

                Click the following link:

                %s

                This link will expire in 15 minutes.

                If you did not request this, please ignore this email.

                Movie Booking Team
                """.formatted(resetLink);

    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken emailToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không tồn tại hoặc không hợp lệ."));

        if (emailToken.getUsed()) {
            throw new RuntimeException("Token đã được sử dụng.");
        }

        if (emailToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn.");
        }

        User userProxy = emailToken.getUser();
        User user = userRepository.findById(userProxy.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));
        user.setEmailVerified(true);
        userRepository.save(user);

        emailToken.setUsed(true);
        emailVerificationTokenRepository.save(emailToken);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Tài khoản đã được xác thực.");
        }

        // Vô hiệu hóa token cũ
        emailVerificationTokenRepository.findByUserAndUsedFalse(user)
                .forEach(t -> t.setUsed(true));

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(java.time.LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(emailToken);

        String verifyLink = "http://localhost:8080/api/auth/verify-email?token=" + token;

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Xác thực tài khoản Cinemind (Gửi lại)",
                    "Chào bạn,\n\nVui lòng click vào link sau để xác thực email của bạn:\n" + verifyLink,
                    false);
        } catch (Exception e) {
            System.err.println("Gửi email thất bại, link xác thực là: " + verifyLink);
        }
    }
}
