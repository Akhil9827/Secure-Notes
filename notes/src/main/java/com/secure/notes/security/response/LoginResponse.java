package com.secure.notes.security.response;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoginResponse {

        private Long id;
        private String jwtToken;
        private String refreshToken;

        private String username;
        private List<String> roles;

        public LoginResponse(Long id,String username, List<String> roles, String jwtToken) {
            this.id=id;
            this.username = username;
            this.roles = roles;
            this.jwtToken = jwtToken;

        }

    public LoginResponse(Long id,String username, List<String> roles, String jwtToken,String refreshToken) {
            this.id=id;
        this.username = username;
        this.roles = roles;
        this.jwtToken = jwtToken;
        this.refreshToken=refreshToken;

    }
    }


