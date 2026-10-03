package com.reservo;

import com.reservo.domain.Role;
import com.reservo.domain.User;
import com.reservo.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test-secret";

    private final JwtService jwt = new JwtService(SECRET, 5);

    @Test
    void roundTripPreservesIdentityAndRole() {
        User u = User.builder().id(7L).email("a@b.com").role(Role.STAFF).build();
        var parsed = jwt.parse(jwt.generate(u)).orElseThrow();
        assertEquals(7L, parsed.id());
        assertEquals("a@b.com", parsed.email());
        assertEquals(Role.STAFF, parsed.role());
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        User u = User.builder().id(1L).email("a@b.com").role(Role.ADMIN).build();
        String token = jwt.generate(u);
        // Altering the payload invalidates the signature (appending to the signature is not reliable:
        // the last base64url character carries unused bits).
        String[] parts = token.split("[.]");
        char first = parts[1].charAt(0);
        String tampered = parts[0] + "." + (first == 'A' ? 'B' : 'A') + parts[1].substring(1) + "." + parts[2];
        assertTrue(jwt.parse(tampered).isEmpty());
        assertTrue(new JwtService("another-secret-another-secret-another-secret-123", 5).parse(token).isEmpty());
        assertTrue(jwt.parse("garbage").isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        User u = User.builder().id(1L).email("a@b.com").role(Role.CUSTOMER).build();
        assertTrue(new JwtService(SECRET, -1).parse(new JwtService(SECRET, -1).generate(u)).isEmpty());
    }
}
