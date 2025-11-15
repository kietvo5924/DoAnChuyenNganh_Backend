package com.example.planmateapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class AvailabilityRequestDto {

    @NotNull(message = "Thời điểm bắt đầu kiểm tra là bắt buộc")
    private OffsetDateTime checkStartTime;

    @NotNull(message = "Thời điểm kết thúc kiểm tra là bắt buộc")
    private OffsetDateTime checkEndTime;
}