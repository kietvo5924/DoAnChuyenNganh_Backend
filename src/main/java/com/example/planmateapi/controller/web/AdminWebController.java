package com.example.planmateapi.controller.web;

import com.example.planmateapi.dto.AdminDashboardAnalytics;
import com.example.planmateapi.dto.CalendarAbuseReportDto;
import com.example.planmateapi.dto.UpdateUserRoleRequestDTO;
import com.example.planmateapi.dto.UserResponseDTO;
import com.example.planmateapi.entity.Role;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.service.AdminService;
import com.example.planmateapi.service.AnalyticsService;
import com.example.planmateapi.service.CalendarAbuseReportService;
import com.example.planmateapi.entity.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminService adminService;
    private final AnalyticsService analyticsService;
    private final CalendarAbuseReportService calendarAbuseReportService;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final Map<ReportStatus, String> REPORT_STATUS_LABELS = Map.of(
            ReportStatus.PENDING, "Đang chờ",
            ReportStatus.WARNED, "Đã cảnh báo",
            ReportStatus.DISMISSED, "Đã đóng",
            ReportStatus.ACTION_TAKEN, "Đã xử lý");

    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        AdminDashboardAnalytics analytics = analyticsService.buildDashboardAnalytics();

        List<String> weeklyLabels = analytics.getWeeklyUserRegistrations().stream()
                .map(metric -> metric.getDate().format(DAY_FORMATTER))
                .collect(Collectors.toList());
        List<Long> weeklyValues = analytics.getWeeklyUserRegistrations().stream()
                .map(AdminDashboardAnalytics.DailyMetric::getCount)
                .collect(Collectors.toList());

        List<String> monthlyLabels = analytics.getMonthlyUserRegistrations().stream()
                .map(metric -> metric.getStart().format(DAY_FORMATTER) + " - " + metric.getEnd().format(DAY_FORMATTER))
                .collect(Collectors.toList());
        List<Long> monthlyValues = analytics.getMonthlyUserRegistrations().stream()
                .map(AdminDashboardAnalytics.WeeklyMetric::getCount)
                .collect(Collectors.toList());

        List<String> reportStatusLabels = analytics.getReportStatusMetrics().stream()
                .map(metric -> REPORT_STATUS_LABELS.getOrDefault(metric.getStatus(), metric.getStatus().name()))
                .collect(Collectors.toList());
        List<Long> reportStatusData = analytics.getReportStatusMetrics().stream()
                .map(AdminDashboardAnalytics.ReportStatusMetric::getCount)
                .collect(Collectors.toList());

        model.addAttribute("analytics", analytics);
        model.addAttribute("weeklyUserLabels", weeklyLabels);
        model.addAttribute("weeklyUserData", weeklyValues);
        model.addAttribute("monthlyUserLabels", monthlyLabels);
        model.addAttribute("monthlyUserData", monthlyValues);
        model.addAttribute("tasksCreatedLabels", List.of("Tuần này", "4 tuần gần nhất"));
        model.addAttribute("tasksCreatedData", List.of(
                analytics.getTasksCreatedThisWeek(),
                analytics.getTasksCreatedThisMonth()));
        model.addAttribute("reportStatusLabels", reportStatusLabels);
        model.addAttribute("reportStatusData", reportStatusData);
        model.addAttribute("contentView", "admin/dashboard");
        model.addAttribute("pageTitle", "Dashboard thống kê");
        model.addAttribute("activeMenu", "dashboard");
        return "fragments/layout";
    }

    @GetMapping("/reports")
    public String listReports(Model model) {
        List<CalendarAbuseReportDto> reports = calendarAbuseReportService.getAllReports();
        model.addAttribute("reports", reports);
        model.addAttribute("contentView", "admin/reports-list");
        model.addAttribute("pageTitle", "Báo cáo vi phạm");
        model.addAttribute("activeMenu", "reports");
        return "fragments/layout";
    }

    @GetMapping("/reports/{id}")
    public String reportDetails(@PathVariable Long id, Model model) {
        CalendarAbuseReportDto report = calendarAbuseReportService.getReportDetail(id);
        model.addAttribute("report", report);
        model.addAttribute("contentView", "admin/report-details");
        model.addAttribute("pageTitle", "Chi tiết báo cáo");
        model.addAttribute("activeMenu", "reports");
        return "fragments/layout";
    }

    @PostMapping("/reports/{id}/warn")
    public String warnViolator(@PathVariable Long id, @RequestParam(required = false) String adminNote,
            RedirectAttributes redirectAttributes) {
        try {
            calendarAbuseReportService.warnViolator(id, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi cảnh báo đến người vi phạm.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/reports/" + id;
    }

    @PostMapping("/reports/{id}/dismiss")
    public String dismissReport(@PathVariable Long id, @RequestParam(required = false) String adminNote,
            RedirectAttributes redirectAttributes) {
        try {
            calendarAbuseReportService.dismissReport(id, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đóng báo cáo.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/reports/" + id;
    }

    @PostMapping("/reports/{id}/lock")
    public String lockViolator(@PathVariable Long id, @RequestParam(required = false) String adminNote,
            RedirectAttributes redirectAttributes) {
        try {
            calendarAbuseReportService.lockViolator(id, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản người vi phạm.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/reports/" + id;
    }

    @GetMapping("/users")
    public String showUsersListPage(Model model) {
        List<User> users = adminService.getAllUsers();
        List<UserResponseDTO> userDTOs = users.stream()
                .map(UserResponseDTO::fromUser)
                .collect(Collectors.toList());

        model.addAttribute("users", userDTOs);
        model.addAttribute("contentView", "admin/users-list");
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        model.addAttribute("activeMenu", "users");
        return "fragments/layout";
    }

    @GetMapping("/users/{id}")
    public String showUserDetailsPage(@PathVariable Long id, Model model) {
        User user = adminService.getUserById(id);

        model.addAttribute("user", UserResponseDTO.fromUser(user));
        model.addAttribute("allRoles", Role.values());
        // Sửa lại đường dẫn view để nhất quán, nếu cần
        model.addAttribute("contentView", "admin/user-details");
        model.addAttribute("pageTitle", "Chi tiết người dùng");
        model.addAttribute("activeMenu", "users");
        return "fragments/layout";
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id, @ModelAttribute UpdateUserRoleRequestDTO requestDTO,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.updateUserRole(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.lockOrUnlockUser(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản thành công!");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.lockOrUnlockUser(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa tài khoản thành công!");
        return "redirect:/admin/users";
    }
}