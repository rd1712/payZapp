package com.payzapp.userservice.dto;

import com.payzapp.userservice.model.AccountStatus;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {

    private String email;
    private UUID userId;
    private String userName;
    private AccountStatus status;
    private String firstName;
    private String lastName;
}
