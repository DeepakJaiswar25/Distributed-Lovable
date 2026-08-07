package com.deepak.distributed_lovable.account_service.controller;


import com.deepak.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.deepak.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.deepak.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.deepak.distributed_lovable.account_service.service.AuthService;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {


    AuthService authService;
//    UserService userService;
    AuthUtil authUtil;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest signupRequest){

        return ResponseEntity.ok(authService.signup(signupRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest){

        return ResponseEntity.ok(authService.login(loginRequest));
    }

//    @GetMapping("/me")
//    public ResponseEntity<AuthResponse> getProfile(){
//        Long userId= authUtil.getCurrentUserId();
//        return ResponseEntity.ok(userService.getProfile(userId));
//    }
}
