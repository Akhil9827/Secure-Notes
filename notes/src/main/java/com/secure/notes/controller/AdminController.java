package com.secure.notes.controller;

import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;
import com.secure.notes.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
    @RequestMapping("/api/admin")
//@PreAuthorize("hasRole('ADMIN')")
    public class AdminController {

        @Autowired
        private UserService userService;

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/getusers")
        public ResponseEntity<List<User>> getAllUsers() {
            List<User> users=userService.getAllUsers();
            return new ResponseEntity<>(users,HttpStatus.OK);
        }


        @PreAuthorize("hasRole('ADMIN')")
        @PutMapping("/update-role")
        public ResponseEntity<String> updateUserRole(@RequestParam Long userId,
                                                     @RequestParam String roleName) {
            String message=userService.updateUserRole(userId, roleName);
            return new ResponseEntity<>(message,HttpStatus.OK);
        }


        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/user/{id}")
        public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
            UserDTO userDTO=userService.getUserById(id);
            return new ResponseEntity<>(userDTO, HttpStatus.OK);
        }

        @PutMapping("/update-lock-status")
        public ResponseEntity<String> updateAccountLockStatus(@RequestParam Long userId,
                                                              @RequestParam boolean lock){
            userService.updateAccountLockStatus(userId,lock);
            return ResponseEntity.ok("Account lock status updated");
        }

        @GetMapping("/roles")
        public ResponseEntity<List<Role>> getAllRoles(){
            List<Role> roles=userService.getAllRoles();
            return new ResponseEntity<>(roles,HttpStatus.OK);
        }

        @PutMapping("/update-expiry-status")
        public ResponseEntity<String> updateAccountExpiryStatus(@RequestParam Long userId,
                                                                @RequestParam boolean expire){
            userService.updateAccountExpiryStatus(userId,expire);
            return ResponseEntity.ok("Account expire status updated");
        }

    @PutMapping("/update-enabled-status")
    public ResponseEntity<String> updateAccountEnabledStatus(@RequestParam Long userId,
                                                             @RequestParam boolean enabled) {
        userService.updateAccountEnabledStatus(userId, enabled);
        return ResponseEntity.ok("Account enabled status updated");
    }

    @PutMapping("/update-credentials-expiry-status")
    public ResponseEntity<String> updateCredentialsExpiryStatus(@RequestParam Long userId,
                                                                @RequestParam boolean expire) {
        userService.updateCredentialsExpiryStatus(userId, expire);
        return ResponseEntity.ok("Credentials expiry status updated");
    }

    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestParam Long userId,
                                                 @RequestParam String password) {
        try {
            userService.updatePassword(userId, password);
            return ResponseEntity.ok("Password updated");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}

