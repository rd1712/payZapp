package com.payzapp.userservice.service;

import com.payzapp.userservice.dto.LoginRequest;
import com.payzapp.userservice.dto.LoginResponse;
import com.payzapp.userservice.dto.RegisterRequest;
import com.payzapp.userservice.dto.RegisterResponse;
import com.payzapp.userservice.exception.InvalidCredentialException;
import com.payzapp.userservice.exception.UserAlreadyExistException;
import com.payzapp.userservice.model.AccountStatus;
import com.payzapp.userservice.model.User;
import com.payzapp.userservice.repository.UserRepository;
import com.payzapp.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistException("Email already exist");
        }
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new UserAlreadyExistException("UserName already exist");
        }
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User savedUser = userRepository.save(User.builder()
                .email(request.getEmail())
                .userName(request.getUserName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordHash).
                status(AccountStatus.INACTIVE)
                .phoneNumber(request.getPhoneNumber())
                .build());

        RegisterResponse response = RegisterResponse.builder()
                .email(savedUser.getEmail())
                .userId(savedUser.getUserId())
                .userName(savedUser.getUserName())
                .status(savedUser.getStatus())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
        return response;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new InvalidCredentialException("Invalid email or password");

        String token = jwtUtil.generateToken(user);

        return LoginResponse.builder()
                .email(user.getEmail())
                .userId(user.getUserId())
                .token(token)
                .build();


    }
}
