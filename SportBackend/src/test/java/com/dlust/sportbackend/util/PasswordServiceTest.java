package com.dlust.sportbackend.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {
    private final PasswordService svc = new PasswordService();

    @Test
    void encode_thenMatches() {
        String hash = svc.encode("dlust123456");
        assertNotEquals("dlust123456", hash);
        assertTrue(svc.matches("dlust123456", hash));
        assertFalse(svc.matches("wrong", hash));
    }

    @Test
    void encode_generatesDifferentHashesForSameInput() {
        // BCrypt 每次加盐，同一明文应产生不同哈希
        assertNotEquals(svc.encode("x"), svc.encode("x"));
    }
}
