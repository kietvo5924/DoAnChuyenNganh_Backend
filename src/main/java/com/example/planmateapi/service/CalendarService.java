package com.example.planmateapi.service;

import com.example.planmateapi.dto.CalendarDto;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.entity.CalendarShare;
import com.example.planmateapi.entity.PermissionLevel;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.CalendarRepository;
import com.example.planmateapi.repository.CalendarShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final AuthenticationService authenticationService;
    private final CalendarShareRepository calendarShareRepository;

    @Transactional
    public CalendarDto.Response createCalendar(CalendarDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        Calendar newCalendar = new Calendar();
        newCalendar.setName(request.getName());
        newCalendar.setDescription(request.getDescription());
        newCalendar.setOwner(currentUser);

        boolean isFirstCalendar = calendarRepository.findByOwnerId(currentUser.getId()).isEmpty();
        if (isFirstCalendar) {
            newCalendar.setDefault(true);
        } else {
            newCalendar.setDefault(request.isDefault());
            if (request.isDefault()) {
                unsetDefaultCalendars(currentUser.getId());
            }
        }

        Calendar savedCalendar = calendarRepository.save(newCalendar);
        return mapToResponse(savedCalendar);
    }

    public List<CalendarDto.Response> getAllCalendarsForCurrentUser() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        return calendarRepository.findByOwnerId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void setDefaultCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendarToSetDefault = findAndVerifyOwnership(calendarId, currentUser);

        unsetDefaultCalendars(currentUser.getId());

        calendarToSetDefault.setDefault(true);
        calendarRepository.save(calendarToSetDefault);
    }

    @Transactional
    public void deleteCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = findAndVerifyOwnership(calendarId, currentUser);

        if (calendar.isDefault()) {
            throw new IllegalStateException("Không thể xóa lịch mặc định. Vui lòng đặt một lịch khác làm mặc định trước.");
        }


        if (calendarRepository.countByOwnerId(currentUser.getId()) <= 1) {
            throw new IllegalStateException("Không thể xóa bộ lịch cuối cùng của bạn.");
        }

        calendarRepository.delete(calendar);
    }

    @Transactional
    public CalendarDto.Response updateCalendar(Long calendarId, CalendarDto.Request request) {
        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật lịch này.");
        }

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));

        calendar.setName(request.getName());
        calendar.setDescription(request.getDescription());

        Calendar updatedCalendar = calendarRepository.save(calendar);
        return mapToResponse(updatedCalendar);
    }

    public CalendarDto.Response getCalendarById(Long calendarId) {
        if (!hasPermission(calendarId, PermissionLevel.VIEW_ONLY)) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch này.");
        }

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        return mapToResponse(calendar);
    }

    private boolean hasPermission(Long calendarId, PermissionLevel requiredPermission) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        Optional<Calendar> calendarOpt = calendarRepository.findById(calendarId);
        if (calendarOpt.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId);
        }
        // 1. Kiểm tra nếu là chủ sở hữu
        if (calendarOpt.get().getOwner().getId().equals(currentUser.getId())) {
            return true;
        }

        // 2. Nếu không phải chủ, kiểm tra xem có được chia sẻ không
        CalendarShare share = calendarShareRepository
                .findByCalendarIdAndSharedWithUserId(calendarId, currentUser.getId())
                .orElse(null);

        if (share == null) {
            return false; // Không được chia sẻ
        }

        if (requiredPermission == PermissionLevel.VIEW_ONLY) {
            return true; // Chỉ cần xem (VIEW_ONLY hoặc EDIT đều được)
        }
        if (requiredPermission == PermissionLevel.EDIT) {
            return share.getPermissionLevel() == PermissionLevel.EDIT; // Phải là quyền EDIT
        }
        return false;
    }

    // Hàm helper để tìm calendar và xác thực quyền sở hữu
    private Calendar findAndVerifyOwnership(Long calendarId, User user) {
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        if (!calendar.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào lịch này.");
        }
        return calendar;
    }

    // Hàm helper để bỏ trạng thái default của tất cả calendar của user
    private void unsetDefaultCalendars(Long userId) {
        List<Calendar> calendars = calendarRepository.findByOwnerId(userId);
        calendars.forEach(cal -> cal.setDefault(false));
        calendarRepository.saveAll(calendars);
    }

    private CalendarDto.Response mapToResponse(Calendar calendar) {
        return new CalendarDto.Response(calendar.getId(), calendar.getName(), calendar.getDescription(), calendar.isDefault());
    }
}
