package com.example.securevault.service;

import com.example.securevault.config.CacheNames;
import com.example.securevault.dto.AuthResponse;
import com.example.securevault.dto.LoginRequest;
import com.example.securevault.dto.RegisterRequest;
import com.example.securevault.dto.UserResponse;
import com.example.securevault.entity.User;
import com.example.securevault.exception.DuplicateEmailException;
import com.example.securevault.exception.InvalidCredentialsException;
import com.example.securevault.exception.UserNotFoundException;
import com.example.securevault.mapper.UserMapper;
import com.example.securevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.USERS, key = "#result.id")
    public UserResponse registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered");
        }

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        User saved = userRepository.save(user);
        log.info("User registered id={} — profile cached", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        String token =
                jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                jwtService.getExpirationMs(),
                userMapper.toResponse(user)
        );
    }

    /**
     * Cached user profile. Method body runs only on cache miss.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USERS, key = "#userId")
    public UserResponse getUserProfile(Long userId) {
        log.info("CACHE MISS users key={} — loading profile from database", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }
}
