package com.clipzy.service;

import com.clipzy.domain.User;
import com.clipzy.dto.AuthResponse;
import com.clipzy.dto.LoginRequest;
import com.clipzy.dto.RegisterRequest;
import com.clipzy.dto.UserResponse;
import com.clipzy.repository.UserRepository;
import com.clipzy.security.JwtService;
import com.clipzy.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
    User user = new User();
    user.setEmail(request.email().trim().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setDisplayName(request.displayName().trim());
    userRepository.save(user);
    return tokensFor(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmailIgnoreCase(request.email().trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return tokensFor(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse refresh(String refreshToken) {
    try {
      var userId = jwtService.requireUserId(refreshToken, JwtService.TYPE_REFRESH);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
      return tokensFor(user);
    } catch (JwtService.JwtAuthException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
  }

  public static UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        user.getCreatedAt()
    );
  }

  private AuthResponse tokensFor(User user) {
    UserPrincipal principal = UserPrincipal.from(user);
    return AuthResponse.of(
        jwtService.createAccessToken(principal),
        jwtService.createRefreshToken(principal),
        toResponse(user)
    );
  }
}
