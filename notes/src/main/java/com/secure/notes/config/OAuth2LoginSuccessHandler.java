package com.secure.notes.config;


import com.secure.notes.exception.APIException;
import com.secure.notes.model.AppRole;
import com.secure.notes.model.RefreshToken;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import com.secure.notes.security.jwt.CookieUtils;
import com.secure.notes.security.jwt.JwtUtils;
import com.secure.notes.security.service.UserDetailsImpl;
import com.secure.notes.security.service.UserDetailsServiceImpl;
import com.secure.notes.services.RefreshTokenService;
import com.secure.notes.services.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
//@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private CookieUtils cookieUtils;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        if ("github".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()) || "google".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())) {
            String username;
            String idAttributeKey;

            DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();
            String email = attributes.getOrDefault("email", "").toString();
            String name = attributes.getOrDefault("name", "").toString();
            if ("github".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())) {
                username = attributes.getOrDefault("login", "").toString();
                idAttributeKey = "id";
            } else if ("google".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())) {
                username = email.split("@")[0];
                idAttributeKey = "sub";
            } else {
                username = "";
                idAttributeKey = "id";
            }
            System.out.println("HELLO OAUTH: " + email + " : " + name + " : " + username);

            userService.findByEmail(email)
                    .ifPresentOrElse(user -> {
                        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                                attributes,
                                idAttributeKey
                        );
                        Authentication securityAuth = new OAuth2AuthenticationToken(
                                oauthUser,
                                List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                                oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
                        );
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    }, () -> {
                        User newUser = new User();
                        Role role = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() ->
                                        new APIException("Default role not found"));

                        newUser.setRole(role);
                        newUser.setEmail(email);
                        newUser.setUserName(username);
                        newUser.setSignUpMethod(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
                        userService.registerUser(newUser);
                        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                                attributes,
                                idAttributeKey
                        );
                        Authentication securityAuth = new OAuth2AuthenticationToken(
                                oauthUser,
                                List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                                oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
                        );
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    });

            this.setAlwaysUseDefaultTargetUrl(true);

//        // JWT TOKEN LOGIC
//        DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
//        Map<String, Object> attributes = oauth2User.getAttributes();
//
//        // Extract necessary attributes
//        String email = (String) attributes.get("email");
//        System.out.println("OAuth2LoginSuccessHandler: " + username + " : " + email);
//
//        Set<SimpleGrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities().stream()
//                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
//                .collect(Collectors.toList()));
//
//        User user=userService.findByEmail(email)
//                        .orElseThrow(()-> new APIException("User not found"));
//        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName().name()));
//
//        // Create UserDetailsImpl instance
//        UserDetailsImpl userDetails = new UserDetailsImpl(
//                null,
//                username,
//                email,
//                null,
//                false,
//                authorities
//        );
//
//
//       // System.out.println("JWT username = " + username);
//       // System.out.println("OAuth authorities = " + oauth2User.getAuthorities());
//
//        // Generate JWT token
//        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
//
//        // Redirect to the frontend with the JWT token
//        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
//                .queryParam("token", jwtToken)
//                .build().toUriString();
//        this.setDefaultTargetUrl(targetUrl);
//        super.onAuthenticationSuccess(request, response, authentication);


            // JWT TOKEN LOGIC
//            DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
//            Map<String, Object> attributes = oauth2User.getAttributes();
//
//            String email = (String) attributes.get("email");



// Fetch the user from the database
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new APIException("User not found"));

            //if 2fa enabled
            if (user.isTwoFactorEnabled()) {

                UserDetailsImpl userDetails =
                        (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUserName());

                String tempToken = jwtUtils.generateTempToken(user.getUserName());

                ResponseCookie tempTokenCookie =
                        cookieUtils.createTempTokenCookie(tempToken);

                response.addHeader(HttpHeaders.SET_COOKIE, tempTokenCookie.toString());

                response.sendRedirect(frontendUrl + "/login?oauth2=true");
                return;
            }

            //if 2fa disabled continue normally
            user.setLastLogin(Instant.now());
            userRepository.save(user);

// Load your application's UserDetails
            UserDetailsImpl userDetails =
                    (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUserName());

// Debug
            System.out.println("========== OAUTH DEBUG ==========");
            System.out.println("GitHub login      : " + username);
            System.out.println("DB username       : " + user.getUserName());
            System.out.println("JWT subject       : " + userDetails.getUsername());
            System.out.println("JWT authorities   : " + userDetails.getAuthorities());
            System.out.println("=================================");

// Generate JWT
            String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

            RefreshToken refreshToken =
                    refreshTokenService.createRefreshToken(user.getUserId());

            ResponseCookie accessTokenCookie = cookieUtils.createAccessTokenCookie(jwtToken);

            ResponseCookie refreshTokenCookie = cookieUtils.createRefreshTokenCookie(refreshToken.getToken());

            // Send cookies to browser
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());


// Redirect
            String targetUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl + "/oauth2/redirect")
                    .build()
                    .toUriString();

            this.setDefaultTargetUrl(targetUrl);
            super.onAuthenticationSuccess(request, response, authentication);

        }


    }
}
