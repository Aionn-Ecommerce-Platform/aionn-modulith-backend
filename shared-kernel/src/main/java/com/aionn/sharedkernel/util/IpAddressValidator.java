package com.aionn.sharedkernel.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpAddressValidator {

    private IpAddressValidator() {
    }

    public static boolean isValid(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        String trimmed = ipAddress.trim();
        if (!isIpLiteral(trimmed)) {
            return false;
        }
        try {
            InetAddress.getByName(trimmed);
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    private static boolean isIpLiteral(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean valid = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '.'
                    || c == ':'
                    || c == '%';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    public static void validate(String ipAddress) {
        if (!isValid(ipAddress)) {
            throw new IllegalArgumentException("Invalid IP address format: " + ipAddress);
        }
    }
}
