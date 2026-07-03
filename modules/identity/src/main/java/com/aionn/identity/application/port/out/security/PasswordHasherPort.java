package com.aionn.identity.application.port.out.security;

public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
