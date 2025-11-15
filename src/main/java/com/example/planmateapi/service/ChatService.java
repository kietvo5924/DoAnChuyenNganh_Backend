package com.example.planmateapi.service;

import com.example.planmateapi.config.AiConfig;
import com.example.planmateapi.dto.AvailabilityResponseDto;
import com.example.planmateapi.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChatService {

    private final ChatClient chatClient;
    private final Function<AiConfig.AvailabilityFunctionRequest, AvailabilityResponseDto> checkAvailabilityFunction;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       Function<AiConfig.AvailabilityFunctionRequest, AvailabilityResponseDto> checkAvailabilityFunction) {

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                    Bạn là trợ lý lịch cá nhân chuyên nghiệp.
                    NGÀY HIỆN TẠI CỦA HỆ THỐNG: 2025-11-15
                    QUY TẮC:
                    1. Bất kỳ câu hỏi nào liên quan đến lịch, rảnh/bận, giờ giấc, PHẢI gọi function: checkAvailabilityFunction.
                    2. KHÔNG trả lời văn bản rằng sẽ gọi function.
                    3. KHÔNG giải thích hay đoán.
                    4. Nếu người dùng nói "ngày mai", "thứ 2 tuần sau", PHẢI tính dựa trên ngày hôm nay.
                    5. checkEndTime luôn = checkStartTime + 1 giờ.
                    FORMAT ISO 8601: 2025-11-16T15:00:00+07:00
                    """)
                .build();

        this.checkAvailabilityFunction = checkAvailabilityFunction;
    }

    public String chat(ChatRequest request) {
        log.info("Gửi truy vấn đến Spring AI: {}", request.message());

        ChatResponse response = chatClient.prompt()
                .user(request.message())
                .call()
                .chatResponse();

        String aiText = response.getResult().getOutput().getText().trim();
        log.info("AI trả về: {}", aiText);

        if (aiText.toLowerCase().startsWith("checkavailabilityfunction(")) {
            try {
                Pattern p = Pattern.compile(
                        "checkAvailabilityFunction\\s*\\(\\s*\"?([^\",\\)]+?)\"?" + // group 1: startTime
                                "\\s*(?:,\\s*\"?([^\",\\)]+?)\"?)?" + // group 2: endTime (tùy chọn)
                                "\\s*\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );

                Matcher m = p.matcher(aiText);
                if (!m.find()) {
                    throw new IllegalArgumentException("Không parse được function call từ AI text: " + aiText);
                }

                String startTime = m.group(1).trim();
                // SỬA LỖI 2: Lấy group 2 (có thể là null nếu AI không trả về)
                String endTime = (m.group(2) != null) ? m.group(2).trim() : null;

                // Thêm +07:00 nếu thiếu timezone cho startTime
                if (!startTime.matches(".*[+-]\\d\\d:\\d\\d$|.*Z$")) {
                    startTime += "+07:00";
                }

                // SỬA LỖI 3: Nếu endTime bị thiếu, tự động tính +1 giờ
                if (endTime == null) {
                    try {
                        log.warn("AI không cung cấp endTime, tự động tính +1 giờ...");
                        OffsetDateTime odt = OffsetDateTime.parse(startTime);
                        OffsetDateTime odtEnd = odt.plusHours(1);
                        endTime = odtEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        log.info("Tính toán endTime: {}", endTime);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Không thể parse startTime để tính endTime: " + startTime);
                    }
                } else {
                    // Nếu endTime tồn tại, chỉ cần kiểm tra timezone
                    if (!endTime.matches(".*[+-]\\d\\d:\\d\\d$|.*Z$")) {
                        endTime += "+07:00";
                    }
                }

                AiConfig.AvailabilityFunctionRequest funcRequest =
                        new AiConfig.AvailabilityFunctionRequest(startTime, endTime);

                AvailabilityResponseDto result = checkAvailabilityFunction.apply(funcRequest);

                if (result.isFree()) {
                    return "Bạn rảnh trong khoảng thời gian này.";
                } else {

                    // Định dạng thời gian cho dễ đọc (ví dụ: "14:59")
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

                    StringBuilder sb = new StringBuilder();
                    int conflictCount = result.getConflicts().size();

                    sb.append("Rất tiếc, bạn đã bận trong khoảng thời gian này. ");

                    if (conflictCount == 1) {
                        sb.append("Có 1 công việc bị trùng: ");
                    } else {
                        sb.append("Có ").append(conflictCount).append(" công việc bị trùng: ");
                    }

                    // Duyệt qua danh sách xung đột để tạo câu văn
                    for (int i = 0; i < result.getConflicts().size(); i++) {
                        AvailabilityResponseDto.ConflictInfo c = result.getConflicts().get(i);

                        // Lấy thời gian bắt đầu đã định dạng
                        String startTimeStr = c.getConflictStartTime().format(timeFormatter);

                        // Thêm "và" vào giữa các công việc
                        if (i > 0) {
                            sb.append(" và ");
                        }

                        sb.append("").append(c.getTaskTitle()).append(""); // Tên công việc
                        sb.append(" từ lịch '").append(c.getCalendarName()).append("'"); // Tên lịch
                        sb.append(" bắt đầu lúc ").append(startTimeStr); // Thời gian
                    }

                    sb.append("."); // Kết thúc câu

                    return sb.toString();
                }

            } catch (Exception e) {
                log.error("Lỗi khi parse function call từ AI: {}", e.getMessage(), e);
                return "Lỗi: Không thể xử lý function call từ AI.";
            }
        }

        return aiText;
    }
}
