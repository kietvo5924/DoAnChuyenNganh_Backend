package com.example.planmateapi.service;

import com.example.planmateapi.dto.*;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarShareService {

    private final CalendarRepository calendarRepository;
    private final CalendarShareRepository calendarShareRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final TagRepository tagRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public CalendarShare shareCalendar(Long calendarId, ShareRequestDto request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = findAndVerifyOwnership(calendarId, currentUser);

        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + request.getEmail()));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể chia sẻ lịch cho chính mình.");
        }

        CalendarShare share = calendarShareRepository
                .findByCalendarIdAndSharedWithUserId(calendarId, targetUser.getId())
                .orElse(CalendarShare.builder()
                        .calendar(calendar)
                        .sharedWithUser(targetUser)
                        .build());

        share.setPermissionLevel(request.getPermissionLevel());
        return calendarShareRepository.save(share);
    }

    @Transactional
    public void unshareCalendar(Long calendarId, Long targetUserId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        findAndVerifyOwnership(calendarId, currentUser);

        if (!userRepository.existsById(targetUserId)){
            throw new ResourceNotFoundException("Không tìm thấy người dùng ID: " + targetUserId);
        }

        calendarShareRepository.deleteByCalendarIdAndSharedWithUserId(calendarId, targetUserId);
    }

    // Chủ sở hữu xem ai đang được chia sẻ
    public List<UserResponseDTO> getUsersSharingCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        findAndVerifyOwnership(calendarId, currentUser);

        List<CalendarShare> shares = calendarShareRepository.findByCalendarId(calendarId);
        return shares.stream()
                .map(share -> UserResponseDTO.fromUser(share.getSharedWithUser()))
                .collect(Collectors.toList());
    }

    // Lấy danh sách lịch ĐƯỢC CHIA SẺ VỚI TÔI
    public List<SharedCalendarResponseDto> getCalendarsSharedWithMe() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        List<CalendarShare> shares = calendarShareRepository.findBySharedWithUserId(currentUser.getId());

        // SỬA LẠI LOGIC MAP
        return shares.stream()
                .map(share -> SharedCalendarResponseDto.from(share.getCalendar(), share.getPermissionLevel()))
                .collect(Collectors.toList());
    }

    // --- HÀM HELPER NỘI BỘ ---

    private Calendar findAndVerifyOwnership(Long calendarId, User user) {
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        if (!calendar.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không phải chủ sở hữu của lịch này.");
        }
        return calendar;
    }

    private CalendarDto.Response mapToCalendarResponse(Calendar calendar) {
        return new CalendarDto.Response(calendar.getId(), calendar.getName(), calendar.getDescription(), calendar.isDefault());
    }
}
