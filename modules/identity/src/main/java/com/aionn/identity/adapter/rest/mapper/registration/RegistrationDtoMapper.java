package com.aionn.identity.adapter.rest.mapper.registration;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.registration.request.CompleteRegistrationRequest;
import com.aionn.identity.adapter.rest.dto.registration.request.InitiateRegistrationRequest;
import com.aionn.identity.adapter.rest.dto.registration.request.VerifyOtpRequest;
import com.aionn.identity.adapter.rest.dto.registration.response.RegistrationSessionResponse;
import com.aionn.identity.adapter.rest.dto.registration.response.VerifyOtpResponse;
import com.aionn.identity.application.dto.registration.command.CompleteRegistrationCommand;
import com.aionn.identity.application.dto.registration.command.InitiateRegistrationCommand;
import com.aionn.identity.application.dto.registration.command.ResendRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.command.VerifyRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.result.InitiateRegistrationResult;
import com.aionn.identity.application.dto.registration.result.ResendRegistrationOtpResult;
import com.aionn.identity.application.dto.registration.result.VerifyRegistrationOtpResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationDtoMapper {

	@Mapping(target = "identity", source = "request.phoneNumber")
	InitiateRegistrationCommand toInitiateCommand(InitiateRegistrationRequest request, String ipAddress);

	RegistrationSessionResponse toInitiateResponse(InitiateRegistrationResult result);

	VerifyRegistrationOtpCommand toVerifyOtpCommand(String regId, VerifyOtpRequest request);

	VerifyOtpResponse toVerifyOtpResponse(VerifyRegistrationOtpResult result);

	CompleteRegistrationCommand toCompleteCommand(
			String regId,
			CompleteRegistrationRequest request,
			String ipAddress,
			String userAgent);

	AuthTokenResponse toAuthTokenResponse(CompleteRegistrationResult result);

	ResendRegistrationOtpCommand toResendOtpCommand(String regId, String ipAddress);

	RegistrationSessionResponse toResendOtpResponse(ResendRegistrationOtpResult result);
}
