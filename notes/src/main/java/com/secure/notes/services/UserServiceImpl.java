package com.secure.notes.services;

import com.secure.notes.exception.APIException;
import com.secure.notes.model.AppRole;
import com.secure.notes.model.PasswordResetToken;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.payload.UserDTO;
import com.secure.notes.repository.PasswordResetTokenRepository;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import com.secure.notes.util.EmailService;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Value("${frontend.url}")
    String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TotpService totpService;

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
                .orElseThrow(() -> new APIException("User not found with userid " + id));
        return modelMapper.map(user, UserDTO.class);
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
                .orElseThrow(() -> new APIException("User not found with user id " + userId));
        user.setAccountNonLocked(!lock);
        userRepository.save(user);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public void updateAccountExpiryStatus(Long userId, boolean expire) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new APIException("user not found with userid " + userId));
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

    @Override
    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new APIException("User not found with " + email));

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);
        PasswordResetToken passwordResetToken = new PasswordResetToken(token, expiryDate, user);

        passwordResetTokenRepository.save(passwordResetToken);

        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        //Send email to user
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new APIException("Invalid password reset token"));
        if (passwordResetToken.isUsed()) {
            throw new APIException("Password reset token has already been used");
        }

        if (passwordResetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new APIException("Password reset token has expired");
        }
        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetToken.setUsed(true);
        passwordResetTokenRepository.save(passwordResetToken);

    }

    @Override
    public Optional<User> findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user;
    }

    @Override
    public User registerUser(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public GoogleAuthenticatorKey generate2FASecret(Long userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new APIException("User not found with user id" + userId));
        GoogleAuthenticatorKey key=totpService.generateSecret();
        user.setTwoFactorSecret(key.getKey());  //key.getKey() extracts only the secret string from the key object beacuse in db we need to store only the secretkey string not whole GoogleAuthenticator Key object
        userRepository.save(user);
        return key;  //you return the entire GoogleAuthenticatorKey because the next step is usually: to generate the qr that requires the key object
    }

    @Override
    public boolean validate2FACode(Long userId, int code){
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new APIException("User not found with userId " + userId));
        return totpService.verifyCode(user.getTwoFactorSecret(), code);
    }

    @Override
    public void enable2FA(Long userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new APIException("User not found with userId " + userId));
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void disable2FA(Long userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new APIException("User not found with userid " + userId));
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
    }

}




