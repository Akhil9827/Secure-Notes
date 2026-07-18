package com.secure.notes.services;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TotpServiceImpl implements TotpService {

    private final GoogleAuthenticator gAuth; //GoogleAuthenticator is a java library we are injecting it directly its constructor injection or else we could have used @Autowired then first we have no make it as a bean

    @Override
    public GoogleAuthenticatorKey generateSecret(){
        return gAuth.createCredentials();
    }

    @Override
    public String getQrCodeUrl(GoogleAuthenticatorKey secret, String username){
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL("Secure Notes Application", username ,secret);
    }

    @Override
    public boolean verifyCode(String secret, int code){
        return gAuth.authorize(secret, code);
    }
}
