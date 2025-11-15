package com.example.planmateapi.service;

import com.example.planmateapi.config.AiConfig;
import com.example.planmateapi.dto.AvailabilityResponseDto;
import com.example.planmateapi.dto.ChatRequest;
import com.example.planmateapi.dto.CreateTaskResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.planmateapi.dto.MessageDto;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChatService {

    private final ChatClient chatClient;
    private final Function<AiConfig.AvailabilityFunctionRequest, AvailabilityResponseDto> checkAvailabilityFunction;
    private final Function<AiConfig.CreateSingleTaskFunctionRequest, CreateTaskResponseDto> createSingleTaskFunction;
    private final Function<AiConfig.CreateRecurringTaskFunctionRequest, CreateTaskResponseDto> createRecurringTaskFunction;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       Function<AiConfig.AvailabilityFunctionRequest, AvailabilityResponseDto> checkAvailabilityFunction,
                       Function<AiConfig.CreateSingleTaskFunctionRequest, CreateTaskResponseDto> createSingleTaskFunction,
                       Function<AiConfig.CreateRecurringTaskFunctionRequest, CreateTaskResponseDto> createRecurringTaskFunction
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                    Bạn là trợ lý lịch cá nhân chuyên nghiệp.
                    NGÀY HIỆN TẠI CỦA HỆ THỐNG: 2025-11-15
                    QUY TẮC:
                    1. Bất kỳ câu hỏi nào liên quan đến lịch, rảnh/bận, giờ giấc, PHẢI gọi function: checkAvailabilityFunction(startTime, endTime).
                    2. Khi người dùng muốn tạo lịch hẹn ĐƠN LẺ, PHẢI gọi: createSingleTaskFunction(title, startTime, endTime, preDayNotify).
                    3. Khi người dùng muốn tạo lịch LẶP LẠI (hàng ngày, hàng tuần...), PHẢI gọi: createRecurringTaskFunction(title, repeatType, repeatStartTime, repeatEndTime, repeatStartDate, repeatDays, repeatInterval, preDayNotify).
                    4. KHÔNG trả lời văn bản rằng sẽ gọi function.
                    5. KHÔNG giải thích hay đoán.
                    6. endTime (cho task đơn) luôn = startTime + 1 giờ.
                    7. repeatEndTime (cho task lặp) luôn = repeatStartTime + 1 giờ.
                    8. Nếu người dùng nói "ngày mai", "thứ 2 tuần sau", PHẢI tính dựa trên ngày hôm nay.

                    QUY TẮC BẮT BUỘC CHO HÀM LẶP LẠI (createRecurringTaskFunction):
                    1. PHẢI LUÔN GỬI ĐỦ 8 THAM SỐ.
                    2. repeatDays (tham số thứ 6):
                       - Dùng cho WEEKLY (VÍ DỤ: "MO", hoặc "SA,SU").
                       - PHẢI là null (chính xác là chữ 'null', KHÔNG có dấu nháy) cho DAILY hoặc MONTHLY.
                    3. repeatInterval (tham số thứ 7): PHẢI là một số. Mặc định là 1.
                    4. preDayNotify (tham số thứ 8): PHẢI là true hoặc false. Mặc định là false.

                    FORMATS:
                    ISO 8601 (cho task đơn): 2025-11-16T15:00:00+07:00
                    LocalTime (cho task lặp): 09:00
                    LocalDate (cho task lặp): 2025-11-17
                    
                    VÍ DỤ KIỂM TRA:
                    User: Chiều mai 3h tôi rảnh không?
                    AI: checkAvailabilityFunction("2025-11-16T15:00:00+07:00", "2025-11-16T16:00:00+07:00")
                    
                    VÍ DỤ TASK ĐƠN:
                    User: Đặt lịch "Khám răng" lúc 9h sáng 20/11, báo trước 1 ngày.
                    AI: createSingleTaskFunction("Khám răng", "2025-11-20T09:00:00+07:00", "2025-11-20T10:00:00+07:00", true)
                    
                    VÍ DỤ TASK LẶP (TUẦN):
                    User: Tạo lịch "Họp team" 9h sáng thứ 2 hàng tuần, bắt đầu từ tuần sau.
                    AI: createRecurringTaskFunction("Họp team", "WEEKLY", "09:00", "10:00", "2025-11-17", "MO", 1, false)

                    VÍ DỤ TASK LẶP (NGÀY):
                    User: Nhắc tôi "Uống thuốc" lúc 8h sáng hàng ngày, bắt đầu từ mai.
                    AI: createRecurringTaskFunction("Uống thuốc", "DAILY", "08:00", "09:00", "2025-11-16", null, 1, false)

                    VÍ DỤ TASK LẶP (THỬ THÁCH):
                    User: Tạo lịch 'Tổng kết' 4h chiều T7, CN, 2 tuần 1 lần, bắt đầu từ tuần này, có báo trước.
                    AI: createRecurringTaskFunction("Tổng kết", "WEEKLY", "16:00", "17:00", "2025-11-15", "SA,SU", 2, true)
                    """)
                .build();

        this.checkAvailabilityFunction = checkAvailabilityFunction;
        this.createSingleTaskFunction = createSingleTaskFunction;
        this.createRecurringTaskFunction = createRecurringTaskFunction;
    }

    public String chat(ChatRequest request) {
        log.info("Gửi truy vấn đến Spring AI: {}", request.message());

        List<Message> allMessages = new ArrayList<>();

        if (request.history() != null) {
            for (MessageDto msg : request.history()) {
                if ("user".equalsIgnoreCase(msg.role())) {
                    allMessages.add(new UserMessage(msg.content()));
                } else if ("assistant".equalsIgnoreCase(msg.role())) {
                    allMessages.add(new AssistantMessage(msg.content()));
                }
            }
        }

        allMessages.add(new UserMessage(request.message()));

        ChatResponse response = chatClient.prompt()
                .messages(allMessages)
                .call()
                .chatResponse();

        String aiText = response.getResult().getOutput().getText().trim();
        log.info("AI trả về: {}", aiText);

        // KHỐI 1: XỬ LÝ KIỂM TRA LỊCH (checkAvailabilityFunction)
        if (aiText.toLowerCase().startsWith("checkavailabilityfunction(")) {
            try {
                // Regex linh hoạt cho 1 hoặc 2 tham số
                Pattern p = Pattern.compile(
                        "checkAvailabilityFunction\\s*\\(\\s*\"?([^\",\\)]+?)\"?" +
                                "\\s*(?:,\\s*\"?([^\",\\)]+?)\"?)?" +
                                "\\s*\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );

                Matcher m = p.matcher(aiText);
                if (!m.find()) {
                    throw new IllegalArgumentException("Không parse được checkAvailabilityFunction: " + aiText);
                }

                String startTime = m.group(1).trim();
                String endTime = (m.group(2) != null) ? m.group(2).trim() : null;

                // Xử lý Timezone và EndTime
                startTime = normalizeAndAddTimezone(startTime);
                endTime = normalizeAndCalculateEndTime(endTime, startTime);


                AiConfig.AvailabilityFunctionRequest funcRequest =
                        new AiConfig.AvailabilityFunctionRequest(startTime, endTime);

                AvailabilityResponseDto result = checkAvailabilityFunction.apply(funcRequest);

                // (Code định dạng câu trả lời "Bạn bận..." của bạn giữ nguyên)
                if (result.isFree()) {
                    return "Bạn rảnh trong khoảng thời gian này.";
                } else {
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                    StringBuilder sb = new StringBuilder();
                    int conflictCount = result.getConflicts().size();
                    sb.append("Rất tiếc, bạn đã bận trong khoảng thời gian này. ");
                    if (conflictCount == 1) {
                        sb.append("Có 1 công việc bị trùng: ");
                    } else {
                        sb.append("Có ").append(conflictCount).append(" công việc bị trùng: ");
                    }
                    for (int i = 0; i < result.getConflicts().size(); i++) {
                        AvailabilityResponseDto.ConflictInfo c = result.getConflicts().get(i);
                        String startTimeStr = c.getConflictStartTime().format(timeFormatter);
                        if (i > 0) {
                            sb.append(" và ");
                        }
                        sb.append("'").append(c.getTaskTitle()).append("'");
                        sb.append(" trên lịch '").append(c.getCalendarName()).append("'");
                        sb.append(", bắt đầu lúc ").append(startTimeStr);
                    }
                    sb.append(".");
                    return sb.toString();
                }

            } catch (Exception e) {
                log.error("Lỗi khi parse checkAvailabilityFunction: {}", e.getMessage(), e);
                return "Lỗi: Không thể xử lý function call (check).";
            }
        }

        // -----------------------------------------------------------------
        // KHỐI 2 (MỚI): XỬ LÝ TẠO LỊCH ĐƠN
        // -----------------------------------------------------------------
        else if (aiText.toLowerCase().startsWith("createsingletaskfunction(")) {
            try {
                // Regex cho 3 tham số (title, start, end). EndTime là tùy chọn.
                Pattern p = Pattern.compile(
                        "createSingleTaskFunction\\s*\\(\\s*\"(.*?)\"\\s*," + // group 1: title
                                "\\s*\"([^\"]+?)\"\\s*" +                       // group 2: startTime
                                "(?:,\\s*\"([^\"]+?)\"\\s*)?" +                // group 3: endTime (tùy chọn)
                                "(?:,\\s*(true|false)\\s*)?" +                 // group 4: preDayNotify (tùy chọn)
                                "\\s*\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );

                Matcher m = p.matcher(aiText);
                if (!m.find()) {
                    throw new IllegalArgumentException("Không parse được createSingleTaskFunction: AI trả về " + aiText);
                }

                String title = m.group(1).trim();
                String startTime = m.group(2).trim();
                String endTime = (m.group(3) != null) ? m.group(3).trim() : null;
                String preDayNotifyStr = (m.group(4) != null) ? m.group(4).trim().toLowerCase() : "false";
                boolean preDayNotify = Boolean.parseBoolean(preDayNotifyStr);

                startTime = normalizeAndAddTimezone(startTime);
                endTime = normalizeAndCalculateEndTime(endTime, startTime);

                // Gọi function bean
                AiConfig.CreateSingleTaskFunctionRequest funcRequest =
                        new AiConfig.CreateSingleTaskFunctionRequest(title, startTime, endTime, preDayNotify);

                CreateTaskResponseDto result = createSingleTaskFunction.apply(funcRequest);

                if (result.success()) {
                    return result.message(); // Trả về câu thông báo từ TaskService
                } else {
                    return "Tạo lịch thất bại: " + result.message();
                }

            } catch (Exception e) {
                log.error("Lỗi khi parse createSingleTaskFunction: {}", e.getMessage(), e);
                return "Lỗi: Không thể xử lý function call (create single).";
            }
        }

        // -----------------------------------------------------------------
        // KHỐI 3: XỬ LÝ TẠO LỊCH LẶP LẠI
        // -----------------------------------------------------------------
        else if (aiText.toLowerCase().startsWith("createrecurringtaskfunction(")) {
            try {
                // Regex BẮT BUỘC 8 tham số, dùng Text Block (dấu """) để tránh lỗi
                Pattern p = Pattern.compile(
                        """
                        createRecurringTaskFunction\\s*\\(\\s*
                        \"(.*?)\"\\s*,\\s*
                        \"(.*?)\"\\s*,\\s*
                        \"(.*?)\"\\s*,\\s*
                        \"(.*?)\"\\s*,\\s*
                        \"(.*?)\"\\s*,\\s*
                        (?:\"([^\"]*?)\"|null)\\s*,\\s*
                        (\\d+)\\s*,\\s*
                        (true|false)
                        \\s*\\)
                        """,
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.COMMENTS
                );

                Matcher m = p.matcher(aiText);
                if (!m.find()) {
                    // Lỗi này xảy ra nếu AI không trả về ĐỦ 8 tham số theo format
                    throw new IllegalArgumentException("Không parse được createRecurringTaskFunction (Regex strict 8): AI trả về " + aiText);
                }

                // Lấy các giá trị (BÂY GIỜ LÀ BẮT BUỘC)
                String title = m.group(1).trim();
                String repeatType = m.group(2).trim();
                String repeatStartTime = m.group(3).trim();
                String repeatEndTime = m.group(4).trim();
                String repeatStartDate = m.group(5).trim();

                // Group 6 là group capturing *bên trong* nhóm non-capturing
                // Nếu AI gửi null (không có nháy), m.group(6) sẽ là null
                // Nếu AI gửi "SA,SU", m.group(6) sẽ là "SA,SU"
                String repeatDays = (m.group(6) != null) ? m.group(6).trim() : null;
                Integer repeatInterval = Integer.parseInt(m.group(7).trim());
                Boolean preDayNotify = Boolean.parseBoolean(m.group(8).trim().toLowerCase());

                // Gọi function bean
                AiConfig.CreateRecurringTaskFunctionRequest funcRequest =
                        new AiConfig.CreateRecurringTaskFunctionRequest(
                                title, repeatType, repeatStartTime, repeatEndTime,
                                repeatStartDate, repeatDays, repeatInterval, preDayNotify
                        );

                CreateTaskResponseDto result = createRecurringTaskFunction.apply(funcRequest);

                if (result.success()) {
                    return result.message(); // Trả về câu thông báo
                } else {
                    return "Tạo lịch lặp lại thất bại: " + result.message();
                }

            } catch (Exception e) {
                log.error("Lỗi khi parse createRecurringTaskFunction: {}", e.getMessage(), e);
                // Thêm chi tiết AI trả về vào lỗi để debug
                return "Lỗi: Không thể xử lý function call (create recurring): " + aiText;
            }
        }

        // Nếu không phải function call, trả text bình thường
        return aiText;
    }

    // --- CÁC HÀM HELPER (TRỢ GIÚP) ---
    // (Thêm 2 hàm này vào cuối class ChatService)

    /**
     * Tự động thêm +07:00 nếu thời gian không chứa múi giờ
     */
    private String normalizeAndAddTimezone(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        if (!dateTimeStr.matches(".*[+-]\\d\\d:\\d\\d$|.*Z$")) {
            return dateTimeStr + "+07:00";
        }
        return dateTimeStr;
    }

    /**
     * Tính endTime = startTime + 1 giờ nếu endTime bị thiếu
     */
    private String normalizeAndCalculateEndTime(String endTimeStr, String startTimeStr) {
        if (endTimeStr != null && !endTimeStr.isBlank()) {
            return normalizeAndAddTimezone(endTimeStr);
        }

        // Nếu endTime rỗng, tính từ startTime
        log.warn("AI không cung cấp endTime (cho task đơn), tự động tính +1 giờ...");
        try {
            OffsetDateTime odt = OffsetDateTime.parse(startTimeStr); // startTimeStr đã có timezone
            OffsetDateTime odtEnd = odt.plusHours(1);
            return odtEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.error("Không thể parse startTime để tính endTime: {}", startTimeStr, e);
            throw new IllegalArgumentException("StartTime không hợp lệ để tính EndTime.");
        }
    }
}
