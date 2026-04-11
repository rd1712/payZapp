package com.payzapp.userservice.dto;

import lombok.*;

import java.util.UUID;
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String email;
    private UUID userId;
    private String token;
}
