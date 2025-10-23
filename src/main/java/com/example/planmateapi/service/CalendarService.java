package com.example.planmateapi.service;

import com.example.planmateapi.dto.CalendarDto;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final AuthenticationService authenticationService;

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
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = findAndVerifyOwnership(calendarId, currentUser);

        calendar.setName(request.getName());
        calendar.setDescription(request.getDescription());

        Calendar updatedCalendar = calendarRepository.save(calendar);
        return mapToResponse(updatedCalendar);
    }

    // Hàm helper để bỏ trạng thái default của tất cả calendar của user
    private void unsetDefaultCalendars(Long userId) {
        List<Calendar> calendars = calendarRepository.findByOwnerId(userId);
        calendars.forEach(cal -> cal.setDefault(false));
        calendarRepository.saveAll(calendars);
    }

    public CalendarDto.Response getCalendarById(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = findAndVerifyOwnership(calendarId, currentUser);
        return mapToResponse(calendar);
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

    private CalendarDto.Response mapToResponse(Calendar calendar) {
        return new CalendarDto.Response(calendar.getId(), calendar.getName(), calendar.getDescription(), calendar.isDefault());
    }
}
