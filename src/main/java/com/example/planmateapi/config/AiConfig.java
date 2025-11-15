package com.example.planmateapi.config;

import com.example.planmateapi.dto.AvailabilityRequestDto;
import com.example.planmateapi.dto.AvailabilityResponseDto;
import com.example.planmateapi.service.AvailabilityService;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.OffsetDateTime;
import java.util.function.Function;

@Configuration
public class AiConfig {

    public record AvailabilityFunctionRequest(
            @JsonPropertyDescription("Thời gian bắt đầu kiểm tra ISO 8601")
            String checkStartTime,
            @JsonPropertyDescription("Thời gian kết thúc kiểm tra ISO 8601")
            String checkEndTime
    ) {}

    @Bean
    @Description("Kiểm tra xem người dùng có rảnh trong khoảng thời gian cụ thể không.")
    public Function<AvailabilityFunctionRequest, AvailabilityResponseDto> checkAvailabilityFunction(
            AvailabilityService availabilityService
    ) {
        return (request) -> {
            AvailabilityRequestDto realRequest = new AvailabilityRequestDto();
            try {
                realRequest.setCheckStartTime(OffsetDateTime.parse(request.checkStartTime()));
                realRequest.setCheckEndTime(OffsetDateTime.parse(request.checkEndTime()));
            } catch (Exception e) {
                return new AvailabilityResponseDto(false,
                        "Lỗi: Định dạng thời gian không hợp lệ (ISO 8601).", null);
            }
            return availabilityService.checkAvailability(realRequest);
        };
    }
}
