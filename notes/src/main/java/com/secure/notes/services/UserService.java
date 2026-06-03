package com.secure.notes.services;

import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {
        String updateUserRole(Long userId, String roleName);

        List<User> getAllUsers();

        UserDTO getUserById(Long id);

        User findByUsername(String username);
}

