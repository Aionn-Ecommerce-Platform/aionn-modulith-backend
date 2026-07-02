package com.aionn.identity.infrastructure.security.password;

import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasherPort {

    private static final int DEFAULT_STRENGTH = 12;
    private static final int MIN_STRENGTH = 10;
    private static final int MAX_STRENGTH = 15;

    private final BCryptPasswordEncoder encoder;

    public BcryptPasswordHasher(
            @Value("${identity.auth.bcrypt-strength:" + DEFAULT_STRENGTH + "}") int strength) {
        int effective = strength;
        if (effective < MIN_STRENGTH || effective > MAX_STRENGTH) {
            effective = DEFAULT_STRENGTH;
        }
        this.encoder = new BCryptPasswordEncoder(effective);
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
