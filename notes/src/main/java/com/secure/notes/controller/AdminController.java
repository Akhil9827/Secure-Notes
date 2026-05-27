package com.secure.notes.controller;

import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;
import com.secure.notes.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
    @RequestMapping("/api/admin")
    public class AdminController {

        @Autowired
        private UserService userService;

        @GetMapping("/getusers")
        public ResponseEntity<List<User>> getAllUsers() {
            List<User> users=userService.getAllUsers();
            return new ResponseEntity<>(users,HttpStatus.OK);
        }

        @PutMapping("/update-role")
        public ResponseEntity<String> updateUserRole(@RequestParam Long userId,
                                                     @RequestParam String roleName) {
            String message=userService.updateUserRole(userId, roleName);
            return new ResponseEntity<>(message,HttpStatus.OK);
        }

        @GetMapping("/user/{id}")
        public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
            UserDTO userDTO=userService.getUserById(id);
            return new ResponseEntity<>(userDTO, HttpStatus.OK);
        }
    }

