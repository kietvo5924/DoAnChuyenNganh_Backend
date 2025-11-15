package com.example.planmateapi.config;

import com.example.planmateapi.dto.AvailabilityRequestDto;
import com.example.planmateapi.dto.AvailabilityResponseDto;
import com.example.planmateapi.dto.CreateTaskResponseDto;
import com.example.planmateapi.entity.RepeatType;
import com.example.planmateapi.service.AvailabilityService;
import com.example.planmateapi.service.TaskService;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.time.LocalTime;
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

    public record CreateSingleTaskFunctionRequest(
            @JsonPropertyDescription("Tiêu đề của công việc hoặc lịch hẹn")
            String title,
            @JsonPropertyDescription("Thời gian bắt đầu ISO 8601")
            String startTime,
            @JsonPropertyDescription("Thời gian kết thúc ISO 8601 (Nếu không có, AI tự tính +1 giờ)")
            String endTime,
            @JsonPropertyDescription("Đặt thông báo trước 1 ngày (true/false). Mặc định là false nếu không nói gì.")
            Boolean preDayNotify
    ) {}

    // 2. Định nghĩa BEAN cho hàm TẠO MỚI
    @Bean
    @Description("Tạo một công việc hoặc lịch hẹn mới cho người dùng. Luôn dùng hàm này khi người dùng yêu cầu 'đặt lịch', 'thêm lịch', 'tạo lịch hẹn'...")
    public Function<CreateSingleTaskFunctionRequest, CreateTaskResponseDto> createSingleTaskFunction(
            TaskService taskService
    ) {
        return (request) -> {
            try {
                // Parse thời gian (Chúng ta sẽ làm logic +1 giờ ở ChatService)
                OffsetDateTime startTime = OffsetDateTime.parse(request.startTime());
                OffsetDateTime endTime = OffsetDateTime.parse(request.endTime());
                boolean notify = (request.preDayNotify() != null) && request.preDayNotify();

                // Gọi service nghiệp vụ
                return taskService.createSingleTaskFromAi(request.title(), startTime, endTime, notify);

            } catch (Exception e) {
                return new CreateTaskResponseDto(false,
                        "Lỗi: Không thể tạo lịch. " + e.getMessage(),
                        request.title(), null, null);
            }
        };
    }

    public record CreateRecurringTaskFunctionRequest(
            @JsonPropertyDescription("Tiêu đề của công việc")
            String title,
            @JsonPropertyDescription("Kiểu lặp, VÍ DỤ: 'WEEKLY', 'DAILY', 'MONTHLY'")
            String repeatType,
            @JsonPropertyDescription("Giờ bắt đầu, VÍ DỤ: '09:00'")
            String repeatStartTime,
            @JsonPropertyDescription("Giờ kết thúc, VÍ DỤ: '10:00'")
            String repeatEndTime,
            @JsonPropertyDescription("Ngày bắt đầu lặp (ISO 8601), VÍ DỤ: '2025-11-20'")
            String repeatStartDate,
            @JsonPropertyDescription("Các ngày lặp (chỉ cho WEEKLY), VÍ DỤ: 'MO,TU,WE'")
            String repeatDays, // Cho phép null
            @JsonPropertyDescription("Khoảng cách lặp (VÍ DỤ: 1 = hàng ngày, 2 = cách ngày)")
            Integer repeatInterval, // Cho phép null
            @JsonPropertyDescription("Đặt thông báo trước 1 ngày (true/false)")
            Boolean preDayNotify // Cho phép null
    ) {}

    @Bean
    @Description("Tạo một công việc LẶP LẠI (hàng ngày, hàng tuần, hàng tháng).")
    public Function<CreateRecurringTaskFunctionRequest, CreateTaskResponseDto> createRecurringTaskFunction(
            TaskService taskService
    ) {
        return (request) -> {
            try {
                // Chuyển đổi các kiểu dữ liệu
                RepeatType type = RepeatType.valueOf(request.repeatType().toUpperCase());
                LocalTime startTime = LocalTime.parse(request.repeatStartTime());
                LocalTime endTime = LocalTime.parse(request.repeatEndTime());
                LocalDate startDate = LocalDate.parse(request.repeatStartDate());

                // Xử lý các giá trị tùy chọn (có thể null)
                int interval = (request.repeatInterval() != null) ? request.repeatInterval() : 1;
                boolean notify = (request.preDayNotify() != null) && request.preDayNotify();
                String days = request.repeatDays(); // có thể null

                // Gọi service
                return taskService.createRecurringTaskFromAi(
                        request.title(), type, startTime, endTime, startDate, days, interval, notify
                );
            } catch (Exception e) {
                return new CreateTaskResponseDto(false,
                        "Lỗi: Không thể tạo lịch lặp lại. " + e.getMessage(),
                        request.title(), null, null);
            }
        };
    }
}
