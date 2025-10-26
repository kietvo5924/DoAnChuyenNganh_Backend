package com.example.planmateapi.dto;

import com.example.planmateapi.entity.PermissionLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareRequestDto {
    @NotBlank(message = "Email người nhận không được trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Cấp độ quyền không được trống")
    private PermissionLevel permissionLevel;
}
