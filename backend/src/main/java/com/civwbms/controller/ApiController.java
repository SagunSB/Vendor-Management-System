package com.civwbms.controller;

import com.civwbms.service.CoreService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final CoreService service;

  public ApiController(CoreService service) {
    this.service = service;
  }

  @GetMapping("/subsidiaries")
  public List<Map<String, Object>> subsidiaries() {
    return service.subsidiaries();
  }

  @GetMapping("/dashboard")
  public Map<String, Object> dashboard(Authentication auth) {
    return service.dashboard(claims(auth));
  }

  @GetMapping("/{module}")
  public List<Map<String, Object>> list(@PathVariable String module, Authentication auth) {
    return service.list(module, claims(auth));
  }

  @PostMapping("/{module}")
  public Map<String, Object> create(@PathVariable String module, @RequestBody Map<String, Object> body, Authentication auth) {
    return service.create(module, body, claims(auth));
  }

  @PutMapping("/{module}/{id}")
  public Map<String, Object> update(@PathVariable String module, @PathVariable long id, @RequestBody Map<String, Object> body, Authentication auth) {
    return service.update(module, id, body, claims(auth));
  }

  @DeleteMapping("/{module}/{id}")
  public Map<String, Object> delete(@PathVariable String module, @PathVariable long id) {
    return service.delete(module, id);
  }

  @GetMapping("/orders/{id}/tracking")
  public List<Map<String, Object>> tracking(@PathVariable long id) {
    return service.tracking(id);
  }

  @PostMapping("/notifications/read")
  public Map<String, Object> read(Authentication auth) {
    service.markNotificationsRead(claims(auth));
    return Map.of("message", "Notifications marked as read");
  }

  private Claims claims(Authentication auth) {
    return (Claims) auth.getCredentials();
  }
}
