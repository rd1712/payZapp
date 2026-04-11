package com.payzapp.userservice.controller;

import com.payzapp.userservice.dto.LoginRequest;
import com.payzapp.userservice.dto.LoginResponse;
import com.payzapp.userservice.dto.RegisterRequest;
import com.payzapp.userservice.dto.RegisterResponse;
import com.payzapp.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public RegisterResponse registerUser(@Valid @RequestBody RegisterRequest request) {

        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
       return  userService.login(request);
    }

}
