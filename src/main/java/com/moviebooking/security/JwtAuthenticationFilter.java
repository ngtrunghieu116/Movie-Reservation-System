package com.moviebooking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // 1. Bỏ qua nếu là request gọi API Auth (Đăng ký/Đăng nhập)
        if (request.getServletPath().contains("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 2. Lấy JWT từ Request Header (thường có dạng "Bearer xyz123...")
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return; // Không có token hợp lệ thì cho qua (sẽ bị chặn lại ở chặn cuối của Spring Security)
        }
        // Cắt bỏ 7 ký tự "Bearer " để lấy đúng chuỗi mã Token
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);
        // 3. Nếu lấy được email từ Token và hiện chưa có ai đăng nhập trong bộ nhớ hệ thống
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Tìm User từ Database thông qua class CustomUserDetailsService ở trên
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            // 4. Nếu Token hợp lệ -> Cài đặt thẻ chứng nhận (Authentication) vào Context
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Báo cho toàn bộ hệ thống Spring biết: "Khách hàng này ĐÃ ĐĂNG NHẬP"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 5. Chuyển Request đi tiếp tới các Filter khác hoặc Controller
        filterChain.doFilter(request, response);
    }
}
