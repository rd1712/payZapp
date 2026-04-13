package com.payzapp.paymentservice.controller;

import com.payzapp.paymentservice.dto.PaymentRequest;
import com.payzapp.paymentservice.dto.PaymentResponse;
import com.payzapp.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping("/api/payment/initiate")
    public PaymentResponse initiatePayment(@Valid @RequestBody PaymentRequest paymentRequest){
       return  paymentService.initiatePayment(paymentRequest);
    }
}
