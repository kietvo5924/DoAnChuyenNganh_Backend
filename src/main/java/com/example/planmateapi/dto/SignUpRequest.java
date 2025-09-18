package com.example.planmateapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng ký tài khoản mới")
public class SignUpRequest {

    @Schema(description = "Họ và tên đầy đủ của người dùng", example = "Nguyễn Văn An")
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Schema(description = "Địa chỉ email hợp lệ", example = "nguyen.van.an@student.edu.vn")
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Schema(description = "Mật khẩu, yêu cầu tối thiểu 6 ký tự", example = "password123")
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
