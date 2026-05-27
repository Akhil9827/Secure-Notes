package com.secure.notes.services;

import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;

import java.util.List;

public interface UserService {
        String updateUserRole(Long userId, String roleName);

        List<User> getAllUsers();

        UserDTO getUserById(Long id);
    }

