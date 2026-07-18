package com.secure.notes.util;

import com.secure.notes.exception.APIException;
import com.secure.notes.model.User;
import com.secure.notes.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    @Autowired
    private UserRepository userRepository;

    public Long loggedInUserId(){
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        User user=userRepository.findByUserName(authentication.getName())
                .orElseThrow(()-> new APIException("User not found"));
        return user.getUserId();
    }

    public User loggedInUser(){
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        User user=userRepository.findByUserName(authentication.getName())
                .orElseThrow(()-> new APIException("User not found"));
        return user;
    }

//    public User loggedInUserName(){
//        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
//        User user=userRepository.findByUserName(authentication.getName())
//                .orElseThrow(()-> new APIException("User not found"));
//        return user.getUserName();
//    }
}
