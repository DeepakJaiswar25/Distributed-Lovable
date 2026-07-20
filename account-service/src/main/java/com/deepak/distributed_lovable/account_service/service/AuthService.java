package com.deepak.distributed_lovable.account_service.service;

import com.deepak.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.deepak.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.deepak.distributed_lovable.account_service.dto.auth.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest signupRequest);

    AuthResponse login(LoginRequest loginRequest);
}
