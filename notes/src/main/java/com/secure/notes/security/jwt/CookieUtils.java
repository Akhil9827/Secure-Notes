package com.secure.notes.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class CookieUtils {

    @Value("${secure.notes.app.jwtCookieName}")
    private String jwtCookieName;

    @Value("${secure.notes.app.refreshCookieName}")
    private String refreshCookieName;

    @Value("${secure.notes.app.tempCookieName}")
    private String tempCookieName;

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        ResponseCookie cookie=  ResponseCookie.from(jwtCookieName, accessToken)
                .path("/api")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(10 * 60 * 1000)  //10 min
                .build();
        return cookie;
    }


    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .path("/api/auth")  //The refresh token is only needed for endpoints like: /api/auth/refresh-token /api/auth/logout
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(7 * 24 * 60 * 60)  //7days
                .build();
        return cookie;
    }

    /// This method is used when: User logs out.
    /// Refresh token has expired or is invalid.
    /// You want to force the user to log in again.

    public ResponseCookie clearAccessTokenCookie() {

        return ResponseCookie.from(jwtCookieName, "")
                .path("/api")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {

        return ResponseCookie.from(refreshCookieName, "")
                .path("/api/auth")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

    //To get Jwt from Cookies
    public String getJwtFromCookies(HttpServletRequest request){
        Cookie cookie= WebUtils.getCookie(request,jwtCookieName);
        if(cookie!=null){
            System.out.println("Cookie "+cookie.getValue());
            return cookie.getValue();
        }
        else{
            return null;
        }
    }

    //Getting RefreshToken From cookies
    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, refreshCookieName);
        if (cookie != null) {
            System.out.println("Refresh Cookie: " + cookie.getValue());
            return cookie.getValue();
        }
        else{
            return null;
        }
    }

    //Generate temp cookie for temp jwt which will use in 2fa verification
    public ResponseCookie createTempTokenCookie(String tempToken) {
        return ResponseCookie.from(tempCookieName, tempToken)
                .path("/api/auth")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(2 * 60)  //2 min
                .build();
    }

    //Get temp token cookie
    public String getTempTokenFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, tempCookieName);
        if (cookie != null) {
            return cookie.getValue();
        } else{
            return null;
        }
    }

    //Clear the temp token cookie after 2fa verified
    public ResponseCookie clearTempTokenCookie() {
        return ResponseCookie.from(tempCookieName, "")
                .path("/api/auth")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

}
