package com.aionn.identity.application.port.out.security;

/**
 * Short-lived replay protection for TOTP values.
 *
 * <p>A TOTP stays valid for the whole time step (and, with a clock-skew window, for the
 * neighbouring steps too). Without a replay guard the same code can be used several times
 * before it expires, which defeats the "one-time" nature of the second factor.</p>
 */
public interface TotpReplayGuardPort {

    /**
     * Atomically claims a TOTP value for a user.
     *
     * @return {@code true} when this call is the first use of the code in the current
     *         validity window; {@code false} when the code was already used (replay) or the
     *         guard store is unavailable. Callers must treat {@code false} as an invalid code.
     */
    boolean claim(String userId, String code);
}
