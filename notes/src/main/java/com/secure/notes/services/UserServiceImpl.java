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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

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

}




