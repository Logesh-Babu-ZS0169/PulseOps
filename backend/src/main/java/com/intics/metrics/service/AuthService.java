package com.intics.metrics.service;

import com.intics.metrics.dto.AuthResponse;
import com.intics.metrics.dto.LoginRequest;
import com.intics.metrics.dto.SignupRequest;
import com.intics.metrics.entity.User;
import com.intics.metrics.repository.UserRepository;
import com.intics.metrics.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Validate JWT token and return the corresponding user if valid.
     *
     * @param token JWT token (e.g., from Authorization header)
     * @return the authenticated User object
     * @throws RuntimeException if token is invalid or user does not exist
     */
    public User validateToken(String authHeader) {
        logger.info("Validating JWT token");

        try {
            // Remove Bearer prefix if present
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

            // Extract username from token
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                throw new RuntimeException("Invalid token: no username found");
            }

            // Fetch user from DB
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found for token"));

            // Validate token for user
            if (!jwtUtil.validateToken(token, user.getUsername())) {
                throw new RuntimeException("Invalid or expired token");
            }

            if (!user.getIsActive()) {
                throw new RuntimeException("User account is inactive");
            }

            logger.info("Token successfully validated for user: {}", username);
            return user;

        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            throw new RuntimeException("Token validation failed: " + e.getMessage());
        }
    }


    public AuthResponse signup(SignupRequest request) {
        logger.info("Processing signup request for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setIsActive(true);

        user = userRepository.save(user);
        logger.info("User created successfully: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        logger.info("Processing login request for username: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Invalid password attempt for username: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is inactive");
        }

        logger.info("User logged in successfully: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName());
    }
}
