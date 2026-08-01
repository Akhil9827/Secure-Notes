package com.secure.notes.controller;

import com.secure.notes.exception.APIException;
import com.secure.notes.model.AppRole;
import com.secure.notes.model.RefreshToken;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.payload.TwoFactorLoginResponse;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import com.secure.notes.security.jwt.CookieUtils;
import com.secure.notes.security.jwt.JwtUtils;
import com.secure.notes.security.request.LoginRequest;
import com.secure.notes.security.request.SignupRequest;
import com.secure.notes.security.request.TokenRefreshRequest;
import com.secure.notes.security.response.LoginResponse;
import com.secure.notes.security.response.MessageResponse;
import com.secure.notes.security.response.TokenRefreshResponse;
import com.secure.notes.security.response.UserInfoResponse;
import com.secure.notes.security.service.UserDetailsImpl;
import com.secure.notes.services.RefreshTokenService;
import com.secure.notes.services.TotpService;
import com.secure.notes.services.UserService;
import com.secure.notes.util.AuthUtil;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private TotpService totpService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CookieUtils cookieUtils;

    @PostMapping("/public/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
        }


        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Load user from database
        User user = userService.findByUsername(userDetails.getUsername());


        // If 2FA is enabled
        if (user.isTwoFactorEnabled()) {

            String tempToken = jwtUtils.generateTempToken(user.getUserName());
            ResponseCookie tempTokenCookie = cookieUtils.createTempTokenCookie(tempToken);

            TwoFactorLoginResponse response = new TwoFactorLoginResponse(true);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, tempTokenCookie.toString())
                    .body(response);
        }

        // User has fully authenticated (no 2FA required)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        // ============================
        // Normal Login

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        // Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        ResponseCookie accessTokenCookie =
                cookieUtils.createAccessTokenCookie(jwtToken);

        ResponseCookie refreshTokenCookie =
                cookieUtils.createRefreshTokenCookie(refreshToken.getToken());


        // Collect roles from the UserDetails
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Prepare the response body, now including the JWT token directly in the body
        LoginResponse response = new LoginResponse(userDetails.getId(),userDetails.getUsername(), roles, jwtToken, refreshToken.getToken());

        // Return the response entity with the JWT token included in the response body
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

    @PostMapping("/public/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Role role;

        if (strRoles == null || strRoles.isEmpty()) {
            role = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new APIException("Error: Role is not found."));
        } else {
            String roleStr = strRoles.iterator().next();
            if (roleStr.equals("admin")) {
                role = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                        .orElseThrow(() -> new APIException("Error: Role is not found."));
            } else {
                role = roleRepository.findByRoleName(AppRole.ROLE_USER)
                        .orElseThrow(() -> new APIException("Error: Role is not found."));
            }

            user.setAccountNonLocked(true);
            user.setAccountNonExpired(true);
            user.setCredentialsNonExpired(true);
            user.setEnabled(true);
            user.setCredentialsExpiryDate(LocalDate.now().plusYears(1));
            user.setAccountExpiryDate(LocalDate.now().plusYears(1));
            user.setTwoFactorEnabled(false);
            user.setSignUpMethod("email");
        }
        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("User registered successfully!"));
    }

    // Add new endpoint for token refresh
    @PostMapping("/public/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {

        String requestRefreshToken = cookieUtils.getRefreshTokenFromCookies(request);

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshToken -> refreshTokenService.verifyExpiration(refreshToken))
                .map(refreshToken -> refreshToken.getUser())
                .map(user -> {
                    UserDetailsImpl userDetails =
                            (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUserName());

                    String newAccessToken = jwtUtils.generateTokenFromUsername(userDetails);
                    // Optional: Generate a new refresh token (rotation)
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUserId());

                    ResponseCookie accessTokenCookie = cookieUtils.createAccessTokenCookie(newAccessToken);

                    ResponseCookie refreshTokenCookie = cookieUtils.createRefreshTokenCookie(newRefreshToken.getToken());

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                            .body(new TokenRefreshResponse(
                                    newAccessToken,
                                    newRefreshToken.getToken()
                            ));
                })
                .orElseThrow(() -> new APIException("Refresh token not found!"));
    }

    // Add logout endpoint to revoke refresh token
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        Long userId = authUtil.loggedInUserId();
        refreshTokenService.deleteByUserId(userId);

        ResponseCookie accessTokenCookie = cookieUtils.clearAccessTokenCookie();
        ResponseCookie refreshTokenCookie = cookieUtils.clearRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new MessageResponse("Logout successful!"));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {

        UserDetailsImpl userDetails=(UserDetailsImpl) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isEnabled(),
                user.getCredentialsExpiryDate(),
                user.getAccountExpiryDate(),
                user.isTwoFactorEnabled(),
                roles,
                user.getLastLogin()
        );

        return ResponseEntity.ok().body(response);
    }
    @GetMapping("/username")     //This Api is used to get current loggedin users name its useful if we are showing it in frontend while the user logged in like Welcome Akhilesh or to display the name on profile icon
    public String currentUserName(Authentication authentication) {  //Spring Security automatically injecting it from SecurityContextHolder
        if (authentication != null) {
            return authentication.getName();
        } else {
            return "Null//Not logged in";
        }
    }

    @PostMapping("/public/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email){
        try {
            userService.generatePasswordResetToken(email);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new MessageResponse("Password reset email sent"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error sending password reset email"));

        }
    }

    @PostMapping("/public/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token,
                                           @RequestParam String newPassword){
        try {
            userService.resetPassword(token, newPassword);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("password reset successful"));
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        }

    }

    @PostMapping("/enable-2fa")
    public ResponseEntity<String> enable2FA(){
        Long userId = authUtil.loggedInUserId();
        GoogleAuthenticatorKey secret= userService.generate2FASecret(userId);
        String qrCodeUrl=totpService.getQrCodeUrl(secret,
                authUtil.loggedInUser().getUserName());
        return ResponseEntity.ok(qrCodeUrl);
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<String> disable2FA(){
        Long userId = authUtil.loggedInUserId();
        userService.disable2FA(userId);
        return ResponseEntity.ok("2FA disabled");
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<String> verify2FA(@RequestParam int code){
        Long userId = authUtil.loggedInUserId();
        boolean isValid=userService.validate2FACode(userId, code);
        if(isValid){
            userService.enable2FA(userId);
            return ResponseEntity.ok("2FA Verified");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid 2FA code");
        }

    }

    @GetMapping("/user/2fa-status")
    public ResponseEntity<?> get2FAStatus(){
        User user = authUtil.loggedInUser();
        if(user!=null){
            return ResponseEntity.ok().body(Map.of("is2faEnabled", user.isTwoFactorEnabled()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

    }

    @PostMapping("/public/verify-2fa-login")
    public ResponseEntity<?> verify2FALogin(@RequestParam int code,
                                            HttpServletRequest httpRequest){

        String jwtToken = cookieUtils.getTempTokenFromCookies(httpRequest);

        if (jwtToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Temporary token not found");
        }

        // Verify this is a temporary token
        if (!"TEMP_2FA".equals(jwtUtils.getTokenType(jwtToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token");
        }

        String username=jwtUtils.getUserNameFromJwtToken(jwtToken);
        User user=userService.findByUsername(username);
        boolean isValid=userService.validate2FACode(user.getUserId(), code);

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid 2FA code");
        }


        user.setLastLogin(Instant.now());
        userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);  //converting user entity into userdetails we already have the build method defined

        /// Yes, it's good practice, because after successful 2FA,
        ///the current request should also reflect that the user is authenticated.
        ///It also keeps this endpoint consistent with your normal login flow
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.generateTokenFromUsername(userDetails);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(userDetails.getId());

        ResponseCookie accessTokenCookie =
                cookieUtils.createAccessTokenCookie(accessToken);

        ResponseCookie refreshTokenCookie = cookieUtils.createRefreshTokenCookie(refreshToken.getToken());

        ResponseCookie clearTempTokenCookie = cookieUtils.clearTempTokenCookie();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        LoginResponse response = new LoginResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles,
                accessToken,
                refreshToken.getToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearTempTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

}


