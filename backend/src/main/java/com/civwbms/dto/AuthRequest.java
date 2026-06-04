package com.civwbms.dto;

import java.util.Map;

public record AuthRequest(String role, String identifier, String password, Long subsidiaryId, Map<String, Object> profile) {}
