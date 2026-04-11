package com.payzapp.userservice.filter;

import com.payzapp.userservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,      // the incoming HTTP request
            HttpServletResponse response,    // the outgoing HTTP response
            FilterChain filterChain          // the rest of the filter chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        System.out.println("JWT Filter running for: " + request.getRequestURI());
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                Claims claim = jwtUtil.validateToken(token);
                System.out.println("Token valid, userId: " + claim.getSubject());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                claim.getSubject(),  // userId as principal
                                null,                // credentials (null after auth)
                                List.of(new SimpleGrantedAuthority("ROLE_" + claim.get("role")))  // authorities
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("SecurityContext set: " + SecurityContextHolder.getContext().getAuthentication());

            } catch (Exception e) {
                System.out.println("Token validation failed: " + e.getMessage());
            }
        }

        System.out.println("Auth before chain: " + SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);// pass to next filter
    }
}
