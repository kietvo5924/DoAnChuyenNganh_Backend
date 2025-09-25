package com.example.planmateapi.service;

import com.example.planmateapi.dto.UpdateUserRoleRequestDTO;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng với ID: " + userId));
    }

    @Transactional
    public User updateUserRole(Long userId, UpdateUserRoleRequestDTO requestDTO) {
        User adminUser = authenticationService.getCurrentAuthenticatedUser();

        if (java.util.Objects.equals(adminUser.getId(), userId)) {
            throw new IllegalArgumentException("Admin không thể tự thay đổi vai trò của chính mình.");
        }

        User userToUpdate = getUserById(userId);

        userToUpdate.setRole(requestDTO.getNewRole());
        return userRepository.save(userToUpdate);
    }

    @Transactional
    public User lockOrUnlockUser(Long userId, boolean lock) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng với ID: " + userId));

        user.setLocked(lock);
        return userRepository.save(user);
    }
}
