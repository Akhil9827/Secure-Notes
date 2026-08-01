package com.secure.notes.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorLoginResponse {

    private boolean twoFactorRequired;
}
