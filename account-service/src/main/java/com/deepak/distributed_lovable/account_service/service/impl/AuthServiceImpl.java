package com.deepak.distributed_lovable.account_service.service.impl;


import com.deepak.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.deepak.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.deepak.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.deepak.distributed_lovable.account_service.entity.User;
import com.deepak.distributed_lovable.account_service.mapper.UserMapper;
import com.deepak.distributed_lovable.account_service.repository.UserRepository;
import com.deepak.distributed_lovable.account_service.service.AuthService;
import com.deepak.distributed_lovable.common_lib.error.BadRequestException;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.deepak.distributed_lovable.common_lib.security.JwtUserPrincipal;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    UserMapper userMapper;
    AuthUtil authUtil;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;

    @Override
    public AuthResponse signup(SignupRequest signupRequest) {
        userRepository.findByUsername(signupRequest.username()).ifPresent(user -> {
            throw new BadRequestException("User already exists with username: "+signupRequest.username());
        });
        User user = userMapper.toEntity(signupRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(user.getId(),user.getName(),
                user.getUsername(),null,new ArrayList<>());
        String token = authUtil.generateAccessToken(jwtUserPrincipal);
        return new AuthResponse(token, userMapper.toUserProfileResponse(jwtUserPrincipal));
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        JwtUserPrincipal user = (JwtUserPrincipal) authentication.getPrincipal();
        String token = authUtil.generateAccessToken(user);
        return new AuthResponse(token, userMapper.toUserProfileResponse(user));
    }
}
