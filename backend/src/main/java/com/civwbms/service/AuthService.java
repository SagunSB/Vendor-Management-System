package com.civwbms.service;

import com.civwbms.config.JwtService;
import com.civwbms.dto.AuthRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthService(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt) {
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  public Map<String, Object> register(AuthRequest req) {
    String role = normalizeRole(req.role());
    Map<String, Object> p = req.profile() == null ? Map.of() : req.profile();
    String hash = encoder.encode(req.password());
    switch (role) {
      case "EMPLOYEE" -> jdbc.update("""
          INSERT INTO employees(employee_code, full_name, email, mobile, subsidiary_id, password_hash, status)
          VALUES(?,?,?,?,?,?, 'ACTIVE')
          """, req.identifier(), p.get("fullName"), p.get("email"), p.get("mobile"), req.subsidiaryId(), hash);
      case "ADMIN" -> jdbc.update("""
          INSERT INTO admins(employee_code, name, email, mobile, subsidiary_id, password_hash)
          VALUES(?,?,?,?,?,?)
          """, req.identifier(), p.get("name"), p.get("email"), p.get("mobile"), req.subsidiaryId(), hash);
      case "HQ" -> jdbc.update("""
          INSERT INTO headquarters(hq_code, name, email, mobile, subsidiary_id, password_hash)
          VALUES(?,?,?,?,?,?)
          """, req.identifier(), p.get("name"), p.get("email"), p.get("mobile"), req.subsidiaryId(), hash);
      case "CLIENT" -> jdbc.update("""
          INSERT INTO clients(client_code, company_name, gst_number, contact_person, email, mobile, password_hash)
          VALUES(?,?,?,?,?,?,?)
          """, req.identifier(), p.get("companyName"), p.get("gstNumber"), p.get("contactPerson"), p.get("email"), p.get("mobile"), hash);
      default -> throw new IllegalArgumentException("Unsupported role");
    }
    return login(req);
  }

  public Map<String, Object> login(AuthRequest req) {
    String role = normalizeRole(req.role());
    String table = switch (role) {
      case "EMPLOYEE" -> "employees";
      case "ADMIN" -> "admins";
      case "HQ" -> "headquarters";
      case "CLIENT" -> "clients";
      default -> throw new IllegalArgumentException("Unsupported role");
    };
    String idCol = switch (role) {
      case "HQ" -> "hq_code";
      case "CLIENT" -> "client_code";
      default -> "employee_code";
    };
    String nameCol = switch (role) {
      case "EMPLOYEE" -> "full_name";
      case "CLIENT" -> "company_name";
      default -> "name";
    };
    String subsidiarySelect = "CLIENT".equals(role) ? "NULL subsidiary_id" : "subsidiary_id";
    String sql = "SELECT id, " + idCol + " identifier, " + nameCol + " name, password_hash, " + subsidiarySelect + " FROM " + table + " WHERE " + idCol + "=?";
    if (!"CLIENT".equals(role)) {
      sql += " AND subsidiary_id=" + req.subsidiaryId();
    }
    Map<String, Object> user = jdbc.queryForMap(sql, req.identifier());
    String storedPassword = String.valueOf(user.get("password_hash"));
    boolean bcryptMatch = encoder.matches(req.password(), storedPassword);
    boolean seedMatch = storedPassword.equals(req.password());
    if (!bcryptMatch && !seedMatch) {
      throw new IllegalArgumentException("Invalid credentials");
    }
    if (seedMatch) {
      jdbc.update("UPDATE " + table + " SET password_hash=? WHERE id=?", encoder.encode(req.password()), user.get("id"));
    }
    Long subsidiaryId = user.get("subsidiary_id") == null ? null : ((Number) user.get("subsidiary_id")).longValue();
    String token = jwt.createToken(((Number) user.get("id")).longValue(), String.valueOf(user.get("identifier")), String.valueOf(user.get("name")), role, subsidiaryId);
    Map<String, Object> body = new HashMap<>();
    body.put("token", token);
    body.put("user", Map.of("id", user.get("id"), "identifier", user.get("identifier"), "name", user.get("name"), "role", role, "subsidiaryId", subsidiaryId));
    return body;
  }

  private String normalizeRole(String role) {
    return role == null ? "" : role.trim().toUpperCase();
  }
}
