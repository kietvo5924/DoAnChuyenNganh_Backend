package com.example.planmateapi.dto;

import com.example.planmateapi.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Yêu cầu để cập nhật vai trò (role) cho một người dùng")
public class UpdateUserRoleRequestDTO {

    @Schema(description = "Vai trò mới muốn gán cho người dùng", example = "USER")
    @NotNull(message = "Vai trò mới không được để trống")
    private Role newRole;
}
