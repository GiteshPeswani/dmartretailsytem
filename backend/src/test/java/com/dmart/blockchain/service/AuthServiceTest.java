package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.request.LoginRequest;
import com.dmart.blockchain.dto.request.RegisterRequest;
import com.dmart.blockchain.dto.response.AuthResponse;
import com.dmart.blockchain.entity.Role;
import com.dmart.blockchain.entity.User;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.repository.RoleRepository;
import com.dmart.blockchain.repository.UserRepository;
import com.dmart.blockchain.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(1L).name(Role.CUSTOMER).build();
    }

    @Test
    @DisplayName("Registration successfully creates customer and generates token")
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Rahul");
        req.setLastName("Sharma");
        req.setEmail("rahul@example.com");
        req.setPassword("Secret@123");

        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(false);
        when(roleRepository.findByName(Role.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        User savedUser = User.builder()
            .id(10L)
            .firstName("Rahul")
            .lastName("Sharma")
            .email("rahul@example.com")
            .roles(Set.of(customerRole))
            .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken("rahul@example.com")).thenReturn("mock-jwt-token");

        AuthResponse resp = authService.register(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("mock-jwt-token");
        assertThat(resp.getEmail()).isEqualTo("rahul@example.com");
        assertThat(resp.getRoles()).contains("CUSTOMER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registration fails when email already exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Login authenticates and returns JWT token")
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@dmart.local");
        req.setPassword("Admin@123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        User user = User.builder()
            .id(1L)
            .email("admin@dmart.local")
            .firstName("Admin")
            .lastName("User")
            .roles(Set.of(Role.builder().id(2L).name("ADMIN").build()))
            .build();

        when(userRepository.findByEmail("admin@dmart.local")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(auth)).thenReturn("admin-token");

        AuthResponse resp = authService.login(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("admin-token");
        assertThat(resp.getRoles()).contains("ADMIN");
    }
}
