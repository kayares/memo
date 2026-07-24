package com.kayares.memo.controller;

import com.kayares.memo.domain.User;
import com.kayares.memo.dto.LoginRequest;
import com.kayares.memo.dto.LoginResponse;
import com.kayares.memo.dto.SignupRequest;
import com.kayares.memo.dto.UserResponse;
import com.kayares.memo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {

        User savedUser = userService.signup(request.getUsername(), request.getPassword());

        return ResponseEntity.ok(new UserResponse(savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        String token = userService.login(request.getUsername(), request.getPassword());

        return ResponseEntity.ok(new LoginResponse(token));
    }
}