package com.aionn.sharedkernel.media;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class CloudinarySigner {

    private static final String SIGNATURE_ALGORITHM = "SHA-1";

    private CloudinarySigner() {
    }

    public static String sign(Map<String, String> params, String apiSecret) {
        String toSign = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        try {
            MessageDigest digest = MessageDigest.getInstance(SIGNATURE_ALGORITHM);
            byte[] hash = digest.digest((toSign + apiSecret).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to compute Cloudinary signature", e);
        }
    }

    public static String uploadUrl(String uploadBaseUrl, String cloudName, String resourceType) {
        return uploadBaseUrl + "/" + cloudName + "/" + resourceType + "/upload";
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
