package com.example.planmateapi.controller.web;

import com.example.planmateapi.dto.UpdateUserRoleRequestDTO;
import com.example.planmateapi.dto.UserResponseDTO;
import com.example.planmateapi.entity.Role;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminService adminService;

    @GetMapping
    public String showUsersListPage(Model model) {
        List<User> users = adminService.getAllUsers();
        List<UserResponseDTO> userDTOs = users.stream()
                .map(UserResponseDTO::fromUser)
                .collect(Collectors.toList());

        model.addAttribute("users", userDTOs);
        model.addAttribute("contentView", "admin/users-list");
        return "fragments/layout";
    }

    @GetMapping("/{id}")
    public String showUserDetailsPage(@PathVariable Long id, Model model) {
        User user = adminService.getUserById(id);

        model.addAttribute("user", UserResponseDTO.fromUser(user));
        model.addAttribute("allRoles", Role.values());
        // Sửa lại đường dẫn view để nhất quán, nếu cần
        model.addAttribute("contentView", "admin/user-details");
        return "fragments/layout";
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id, @ModelAttribute UpdateUserRoleRequestDTO requestDTO, RedirectAttributes redirectAttributes) {
        try {
            adminService.updateUserRole(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/lock")
    public String lockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.lockOrUnlockUser(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản thành công!");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.lockOrUnlockUser(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa tài khoản thành công!");
        return "redirect:/admin/users";
    }
}