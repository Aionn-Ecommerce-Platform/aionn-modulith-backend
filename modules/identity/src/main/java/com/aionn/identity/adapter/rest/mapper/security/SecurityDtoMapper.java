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

import java.util.List;

@Mapper(componentModel = "spring")
public interface SecurityDtoMapper {

    // Request -> Command
    ChangePasswordCommand toChangePasswordCommand(String userId, String clientIp, ChangePasswordRequest request);

    RequestPasswordResetCommand toPasswordResetCommand(String clientIp, PasswordResetRequestCommand request);

    CompletePasswordResetCommand toCompletePasswordResetCommand(String clientIp, CompletePasswordResetRequest request);

    InitiateMfaSetupCommand toInitiateMfaSetupCommand(String userId, String clientIp, MfaSetupRequest request);

    EnableMfaCommand toEnableMfaCommand(String userId, String clientIp, MfaToggleRequest request);

    DisableMfaCommand toDisableMfaCommand(String userId, String clientIp, MfaToggleRequest request);

    RegenerateBackupCodesCommand toRegenerateBackupCodesCommand(String userId, String password, String mfaCode,
            String clientIp);

    UnlockAccountCommand toUnlockAccountCommand(String userId);

    // Result -> Response
    PasswordResetResponse toPasswordResetResponse(PasswordResetResult result);

    MfaSetupResponse toMfaSetupResponse(MfaSetupResult result);

    MfaResponse toMfaResponse(MfaResult result);

    BackupCodesResponse toBackupCodesResponse(BackupCodesResult result);

    SecurityAuditLogResponse toAuditLogRow(SecurityAuditLogResult audit);

    List<SecurityAuditLogResponse> toAuditLogResponse(List<SecurityAuditLogResult> logs);
}
