package com.payzapp.paymentservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String token = JwtTokenHolder.getToken();
        if (token != null) {
            template.header("Authorization", "Bearer " + token);
        }
    }
}