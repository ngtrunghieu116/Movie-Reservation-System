package com.moviebooking.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TheaterRequest {

    @NotBlank(message = "Tên rạp/cơ sở không được để trống")
    @Size(max = 100, message = "Tên rạp không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 200, message = "Địa chỉ không được vượt quá 200 ký tự")
    private String address;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100, message = "Thành phố không được vượt quá 100 ký tự")
    private String city;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
    private String district;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[2-9]|1800|1900)[0-9]{7,9}$", message = "Số điện thoại không hợp lệ (hỗ trợ số di động, số bàn hoặc hotline 1800/1900)")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String description;

    private Boolean isActive = true;
}
