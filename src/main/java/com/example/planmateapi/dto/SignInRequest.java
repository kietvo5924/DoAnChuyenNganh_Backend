package com.example.planmateapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng nhập")
public class SignInRequest {

    @Schema(description = "Email đã đăng ký", example = "nguyenvana@example.com")
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Schema(description = "Mật khẩu của người dùng", example = "password123")
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
