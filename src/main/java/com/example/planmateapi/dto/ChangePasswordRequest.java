package com.example.planmateapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đổi mật khẩu")
public class ChangePasswordRequest {

    @Schema(description = "Mật khẩu hiện tại của người dùng", example = "password123")
    @NotBlank(message = "Mật khẩu hiện tại là bắt buộc")
    private String currentPassword;

    @Schema(description = "Mật khẩu mới (ít nhất 6 ký tự)", example = "newpassword456")
    @NotBlank(message = "Mật khẩu mới là bắt buộc")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;

    @Schema(description = "Xác nhận mật khẩu mới", example = "newpassword456")
    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
    private String confirmationPassword;
}
