package com.agnitio.infrastructure.security;

import com.agnitio.application.ports.output.PasswordEncoderPort;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder delegate;

    public Argon2PasswordEncoderAdapter() {
        this.delegate = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
