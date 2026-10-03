package com.reservo.service;

import com.reservo.domain.AuthProvider;
import com.reservo.domain.Role;
import com.reservo.domain.User;
import com.reservo.dto.Dtos.*;
import com.reservo.exception.ApiException;
import com.reservo.repo.UserRepository;
import com.reservo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = users.save(User.builder()
                .fullName(req.fullName().trim())
                .email(req.email().trim().toLowerCase())
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.CUSTOMER) // public sign-up can only ever create customers
                .provider(AuthProvider.LOCAL)
                .build());
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email())
                .filter(u -> u.getPasswordHash() != null && encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled");
        }
        return response(user);
    }

    @Transactional
    public User findOrCreateOAuthUser(String email, String name) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> users.save(User.builder()
                .fullName(name == null || name.isBlank() ? email : name)
                .email(email.toLowerCase())
                .role(Role.CUSTOMER)
                .provider(AuthProvider.GOOGLE)
                .build()));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long id) {
        return users.findById(id).map(UserDto::from).orElseThrow(() -> ApiException.notFound("User"));
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.generate(user), UserDto.from(user));
    }
}
