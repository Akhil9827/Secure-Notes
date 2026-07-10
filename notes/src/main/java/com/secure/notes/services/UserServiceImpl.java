package com.secure.notes.services;

import com.secure.notes.exception.APIException;
import com.secure.notes.model.AppRole;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public String updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new APIException("User not found"));
        AppRole appRole = AppRole.valueOf(roleName);//AppRole.valueOf(roleName) converts the role name string into its corresponding enum constant so it can be used in type-safe enum-based operations.

        Role role = roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new APIException("Role not found"));
        user.setRole(role);
        userRepository.save(user);
        return "User role updated successfully";
    }


    //@PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(()-> new APIException("User not found with userid " + id));
       return modelMapper.map(user,UserDTO.class);
    }

    @Override
    public User findByUsername(String username) {
       User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new APIException("User not found with username: " + username));
                return user;
    }

    @Override
    public void updateAccountLockStatus(Long userId, boolean lock) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new APIException("User not found with user id " + userId ));
        user.setAccountNonLocked(!lock);
        userRepository.save(user);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public void updateAccountExpiryStatus(Long userId, boolean expire) {
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new APIException("user not found with userid " + userId));
        user.setAccountNonExpired(!expire);
        userRepository.save(user);
    }

    @Override
    public void updateAccountEnabledStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new APIException("User not found with userid " + userId));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public void updateCredentialsExpiryStatus(Long userId, boolean expire) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new APIException("User not found with userid" + userId));
        user.setCredentialsNonExpired(!expire);
        userRepository.save(user);
    }

    @Override
    public void updatePassword(Long userId, String password) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new APIException("User not found"));
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        } catch (Exception e) {
            throw new APIException("Failed to update password");
        }
    }

}




