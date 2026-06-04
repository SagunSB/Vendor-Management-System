package com.civwbms.controller;

import com.civwbms.dto.AuthRequest;
import com.civwbms.service.AuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
    return ResponseEntity.ok(auth.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
    return ResponseEntity.ok(auth.login(request));
  }
}
