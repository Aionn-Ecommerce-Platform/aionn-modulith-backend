package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.application.port.out.consent.ConsentPersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.UserConsent;
import com.aionn.identity.domain.valueobject.ConsentType;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.sharedkernel.util.IpAddressValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConsentService {

    public static final String DEFAULT_MARKETING_VERSION = "marketing-v1";

    private final UserPersistencePort userPersistencePort;
    private final ConsentPersistencePort consentPersistencePort;

    public ConsentResult agreeTerms(String userId, String version, String ipAddress) {
        log.info("Recording terms consent for user: {}, version: {}", userId, version);
        return appendDecision(userId, ConsentType.TERMS, version, ipAddress, true);
    }

    public ConsentResult agreePrivacy(String userId, String version, String ipAddress) {
        log.info("Recording privacy consent for user: {}, version: {}", userId, version);
        return appendDecision(userId, ConsentType.PRIVACY, version, ipAddress, true);
    }

    public ConsentResult updateMarketing(String userId, boolean subscribed, String ipAddress) {
        log.info("Updating marketing consent for user: {}, subscribed: {}", userId, subscribed);
        return appendDecision(userId, ConsentType.MARKETING, DEFAULT_MARKETING_VERSION, ipAddress, subscribed);
    }

    @Transactional(readOnly = true)
    public List<ConsentResult> listMy(String userId) {
        log.debug("Listing consents for user: {}", userId);
        userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        return consentPersistencePort.findHistory(userId);
    }

    private ConsentResult appendDecision(
            String userId,
            ConsentType consentType,
            String version,
            String ipAddress,
            boolean granted) {

        if (ipAddress != null && !ipAddress.isBlank() && !IpAddressValidator.isValid(ipAddress)) {
            log.warn("Invalid IP address format provided: {}", ipAddress);
            throw new IdentityException(IdentityErrorCode.INVALID_IP_ADDRESS);
        }

        userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        LocalDateTime now = nowUtc();
        UserConsent consent = UserConsent.builder()
                .id(IdGenerator.ulid())
                .userId(userId)
                .consentType(consentType)
                .version(version)
                .granted(granted)
                .agreedAt(now)
                .revokedAt(granted ? null : now)
                .ipAddress(ipAddress)
                .build();

        ConsentResult saved = consentPersistencePort.append(consent);
        log.info("Consent appended for user: {}, type: {}, granted: {}", userId, consentType, granted);
        return saved;
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
