package com.secure.notes.services;

import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {
        String updateUserRole(Long userId, String roleName);

        List<User> getAllUsers();

        UserDTO getUserById(Long id);

        User findByUsername(String username);

    void updateAccountLockStatus(Long userId, boolean lock);

    List<Role> getAllRoles();

    void updateAccountExpiryStatus(Long userId, boolean expire);

    void updateAccountEnabledStatus(Long userId, boolean enabled);

    void updateCredentialsExpiryStatus(Long userId, boolean expire);

    void updatePassword(Long userId, String password);

    void generatePasswordResetToken(String email);

    void resetPassword(String token, String newPassword);
}

