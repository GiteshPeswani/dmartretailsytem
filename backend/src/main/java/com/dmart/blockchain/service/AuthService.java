package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.request.LoginRequest;
import com.dmart.blockchain.dto.request.RegisterRequest;
import com.dmart.blockchain.dto.response.AuthResponse;
import com.dmart.blockchain.dto.response.UserProfileResponse;
import com.dmart.blockchain.entity.Role;
import com.dmart.blockchain.entity.User;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.RoleRepository;
import com.dmart.blockchain.repository.UserRepository;
import com.dmart.blockchain.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email is already registered: " + req.getEmail());
        }

        // Enforce: Public registration always assigns CUSTOMER role only
        Role customerRole = roleRepository.findByName(Role.CUSTOMER)
            .orElseGet(() -> roleRepository.save(Role.builder().name(Role.CUSTOMER).build()));

        User user = User.builder()
            .firstName(req.getFirstName().trim())
            .lastName(req.getLastName().trim())
            .email(req.getEmail().trim().toLowerCase())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .phone(req.getPhone())
            .enabled(true)
            .roles(new HashSet<>(Set.of(customerRole)))
            .build();

        User saved = userRepository.save(user);
        log.info("Registered new customer user: {}", saved.getEmail());

        String token = tokenProvider.generateToken(saved.getEmail());
        return buildAuthResponse(saved, token);
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail().trim().toLowerCase(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = tokenProvider.generateToken(auth);
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        return UserProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
            .createdAt(user.getCreatedAt())
            .build();
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
            .build();
    }
}
