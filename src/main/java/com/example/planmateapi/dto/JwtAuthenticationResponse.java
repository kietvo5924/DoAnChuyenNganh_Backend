package com.example.planmateapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Đối tượng trả về sau khi đăng nhập thành công")
public class JwtAuthenticationResponse {

    @Schema(description = "JWT Access Token để xác thực cho các yêu cầu sau này",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0LnVzZXJAZXhhbXBsZS5jb20iLCJpYXQiOjE3MTgxNjY4MDksImV4cCI6MTcxODI1MzIwOX0.abc...")
    private String token;
}
