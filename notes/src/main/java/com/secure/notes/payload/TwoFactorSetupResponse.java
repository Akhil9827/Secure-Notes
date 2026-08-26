package com.secure.notes.payload;

import com.secure.notes.model.User;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {
    private GoogleAuthenticatorKey key;
    private User user;
}
