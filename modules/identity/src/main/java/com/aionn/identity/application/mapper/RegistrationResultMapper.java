package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.result.InitiateRegistrationResult;
import com.aionn.identity.application.dto.registration.result.ResendRegistrationOtpResult;
import com.aionn.identity.application.dto.registration.result.VerifyRegistrationOtpResult;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.RegistrationVerificationSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface RegistrationResultMapper {

    InitiateRegistrationResult toInitiateResult(RegistrationVerificationSession session, String otpCode);

    VerifyRegistrationOtpResult toVerifyOtpResult(String regId, String verificationToken);

    ResendRegistrationOtpResult toResendOtpResult(RegistrationVerificationSession session, String otpCode);

    @Mapping(target = "userId", source = "session.userId")
    @Mapping(target = "sessionId", source = "session.sessionId")
    @Mapping(target = "sessionExpiresAt", source = "session.expiresAt")
    CompleteRegistrationResult toCompleteResult(
            AuthSession session,
            String accessToken,
            String refreshToken,
            Instant expiresAt);
}
