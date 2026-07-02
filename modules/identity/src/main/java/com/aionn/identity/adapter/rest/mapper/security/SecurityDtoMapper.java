package com.aionn.identity.adapter.rest.mapper.security;

import com.aionn.identity.adapter.rest.dto.security.request.ChangePasswordRequest;
import com.aionn.identity.adapter.rest.dto.security.request.CompletePasswordResetRequest;
import com.aionn.identity.adapter.rest.dto.security.request.MfaSetupRequest;
import com.aionn.identity.adapter.rest.dto.security.request.MfaToggleRequest;
import com.aionn.identity.adapter.rest.dto.security.request.PasswordResetRequestCommand;
import com.aionn.identity.adapter.rest.dto.security.response.BackupCodesResponse;
import com.aionn.identity.adapter.rest.dto.security.response.MfaResponse;
import com.aionn.identity.adapter.rest.dto.security.response.MfaSetupResponse;
import com.aionn.identity.adapter.rest.dto.security.response.PasswordResetResponse;
import com.aionn.identity.adapter.rest.dto.security.response.SecurityAuditLogResponse;
import com.aionn.identity.application.dto.security.result.BackupCodesResult;
import com.aionn.identity.application.dto.security.command.ChangePasswordCommand;
import com.aionn.identity.application.dto.security.command.EnableMfaCommand;
import com.aionn.identity.application.dto.security.command.InitiateMfaSetupCommand;
import com.aionn.identity.application.dto.security.result.MfaResult;
import com.aionn.identity.application.dto.security.result.MfaSetupResult;
import com.aionn.identity.application.dto.security.result.SecurityAuditLogResult;
import com.aionn.identity.application.dto.security.command.CompletePasswordResetCommand;
import com.aionn.identity.application.dto.security.command.DisableMfaCommand;
import com.aionn.identity.application.dto.security.command.RegenerateBackupCodesCommand;
import com.aionn.identity.application.dto.security.command.RequestPasswordResetCommand;
import com.aionn.identity.application.dto.security.command.UnlockAccountCommand;
import com.aionn.identity.application.dto.security.result.PasswordResetResult;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SecurityDtoMapper {

    // Request -> Command
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "currentPassword", source = "request.currentPassword")
    @Mapping(target = "newPassword", source = "request.newPassword")
    @Mapping(target = "clientIp", source = "clientIp")
    ChangePasswordCommand toChangePasswordCommand(String userId, String clientIp, ChangePasswordRequest request);

    @Mapping(target = "identity", source = "request.identity")
    @Mapping(target = "clientIp", source = "clientIp")
    RequestPasswordResetCommand toPasswordResetCommand(String clientIp, PasswordResetRequestCommand request);

    @Mapping(target = "token", source = "request.token")
    @Mapping(target = "newPassword", source = "request.newPassword")
    @Mapping(target = "clientIp", source = "clientIp")
    CompletePasswordResetCommand toCompletePasswordResetCommand(String clientIp, CompletePasswordResetRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "password", source = "request.password")
    @Mapping(target = "clientIp", source = "clientIp")
    InitiateMfaSetupCommand toInitiateMfaSetupCommand(String userId, String clientIp, MfaSetupRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "password", source = "request.password")
    @Mapping(target = "mfaCode", source = "request.mfaCode")
    @Mapping(target = "clientIp", source = "clientIp")
    EnableMfaCommand toEnableMfaCommand(String userId, String clientIp, MfaToggleRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "password", source = "request.password")
    @Mapping(target = "mfaCode", source = "request.mfaCode")
    @Mapping(target = "clientIp", source = "clientIp")
    DisableMfaCommand toDisableMfaCommand(String userId, String clientIp, MfaToggleRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "mfaCode", source = "mfaCode")
    @Mapping(target = "clientIp", source = "clientIp")
    RegenerateBackupCodesCommand toRegenerateBackupCodesCommand(String userId, String password, String mfaCode, String clientIp);

    UnlockAccountCommand toUnlockAccountCommand(String userId);

    // Result -> Response
    @Mapping(target = "message", source = "message")
    PasswordResetResponse toPasswordResetResponse(PasswordResetResult result);

    @Mapping(target = "secret", source = "secret")
    @Mapping(target = "otpauthUri", source = "otpauthUri")
    @Mapping(target = "issuer", source = "issuer")
    @Mapping(target = "accountName", source = "accountName")
    MfaSetupResponse toMfaSetupResponse(MfaSetupResult result);

    @Mapping(target = "mfaEnabled", source = "mfaEnabled")
    @Mapping(target = "backupCodes", source = "backupCodes")
    MfaResponse toMfaResponse(MfaResult result);

    @Mapping(target = "backupCodes", source = "backupCodes")
    BackupCodesResponse toBackupCodesResponse(BackupCodesResult result);

    @Mapping(target = "auditId", source = "auditId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "timestamp", source = "timestamp")
    SecurityAuditLogResponse toAuditLogRow(SecurityAuditLogResult audit);

    List<SecurityAuditLogResponse> toAuditLogResponse(List<SecurityAuditLogResult> logs);
}
