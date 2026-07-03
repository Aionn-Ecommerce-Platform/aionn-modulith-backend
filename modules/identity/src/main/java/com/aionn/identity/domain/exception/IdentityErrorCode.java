package com.aionn.identity.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IdentityErrorCode {
	PHONE_ALREADY_EXISTS("IDENTITY_001", "Phone number already exists in the system"),
	EMAIL_ALREADY_EXISTS("IDENTITY_002", "Email already exists in the system"),
	EMAIL_REQUIRED("IDENTITY_003", "Email is required"),
	PHONE_REQUIRED("IDENTITY_004", "Phone number is required"),
	USERNAME_ALREADY_EXISTS("IDENTITY_005", "Username already exists in the system"),
	PHONE_INVALID("IDENTITY_006", "Invalid phone number format"),

	OTP_INVALID("IDENTITY_101", "Invalid OTP code"),
	OTP_EXPIRED("IDENTITY_102", "OTP code has expired"),
	OTP_ATTEMPTS_EXCEEDED("IDENTITY_103", "OTP attempts exceeded the allowed limit"),
	OTP_RESEND_TOO_SOON("IDENTITY_104", "Please wait before requesting another OTP"),
	CAPTCHA_INVALID("IDENTITY_105", "Invalid captcha token"),
	VERIFICATION_TOKEN_INVALID("IDENTITY_106", "Verification Token invalid"),
	RATE_LIMIT_EXCEEDED("IDENTITY_107", "Rate limit exceeded"),
	ADDRESS_NUMBER_EXCEEDED("IDENTITY_108", "Too many addresses, maximum 5 allowed"),
	REGISTRATION_SESSION_NOT_FOUND("IDENTITY_109", "Registration session not found"),
	REGISTRATION_ALREADY_VERIFIED("IDENTITY_110", "Registration already verified"),
	REGISTRATION_SESSION_EXPIRED("IDENTITY_111", "Registration session has expired"),
	REGISTRATION_IN_PROGRESS("IDENTITY_112", "Registration is already in progress for this phone number"),

	USER_NOT_FOUND("IDENTITY_201", "User not found"),
	INVALID_DISPLAY_NAME("IDENTITY_202", "Invalid display name"),
	INVALID_CREDENTIALS("IDENTITY_203", "Invalid credentials"),
	USER_INACTIVE("IDENTITY_204", "User account is not active"),
	SESSION_NOT_FOUND("IDENTITY_205", "Session not found"),
	SESSION_FORBIDDEN("IDENTITY_206", "Session does not belong to current user"),
	INSUFFICIENT_PERMISSIONS("IDENTITY_220", "Insufficient permissions to perform this operation"),
	SOCIAL_LINK_EXISTS("IDENTITY_207", "Social account is already linked"),
	SOCIAL_LINK_NOT_FOUND("IDENTITY_208", "Social account link not found"),
	PROVIDER_NOT_SUPPORTED("IDENTITY_209", "Social provider is not supported"),
	PROVIDER_TOKEN_INVALID("IDENTITY_210", "Social provider token is invalid"),
	OTP_REQUIRED("IDENTITY_211", "OTP code is required"),
	OTP_CHANNEL_NOT_SUPPORTED("IDENTITY_212", "OTP channel is not supported"),
	AVATAR_URL_INVALID("IDENTITY_213", "Avatar URL is invalid"),
	ACCOUNT_DELETION_ALREADY_REQUESTED("IDENTITY_214", "Account deletion request already exists"),
	ACCOUNT_DELETION_NOT_FOUND("IDENTITY_215", "Account deletion request not found"),
	DATA_EXPORT_ALREADY_IN_PROGRESS("IDENTITY_216", "A data export request is already in progress"),
	EMAIL_VERIFICATION_NOT_FOUND("IDENTITY_217", "Email verification challenge not found"),
	EMAIL_CHANGE_NOT_FOUND("IDENTITY_218", "Email change challenge not found"),
	PHONE_CHANGE_NOT_FOUND("IDENTITY_219", "Phone change challenge not found"),
	MFA_ALREADY_ENABLED("IDENTITY_221", "MFA is already enabled"),
	MFA_NOT_ENABLED("IDENTITY_222", "MFA is not enabled"),
	MFA_SETUP_NOT_INITIATED("IDENTITY_223", "MFA setup has not been initiated"),
	AUTHENTICATION_REQUIRED("IDENTITY_224", "Authentication required"),
	ACCESS_DENIED("IDENTITY_225", "Access denied"),
	INVALID_USER_STATUS("IDENTITY_226", "Invalid user status"),
	INVALID_USER_ROLE("IDENTITY_227", "Invalid user role"),
	INVALID_FEEDBACK_CATEGORY("IDENTITY_228", "Invalid feedback category"),
	FEEDBACK_CONTENT_REQUIRED("IDENTITY_229", "Feedback content is required"),

	INVALID_ADDRESS_TYPE("IDENTITY_301", "Invalid address type. Supported types are HOME or OFFICE"),
	ADDRESS_NOT_FOUND("IDENTITY_302", "Address not found"),
	DEFAULT_ADDRESS_CANNOT_BE_DELETED("IDENTITY_303",
			"Default address cannot be deleted. Please set another address as default first"),
	INVALID_GEOGRAPHY_CODE("IDENTITY_304", "Invalid geography code"),

	KYC_NOT_FOUND("IDENTITY_401", "KYC profile not found"),
	KYC_INVALID_STATUS_TRANSITION("IDENTITY_402", "Invalid KYC status transition"),
	KYC_CANNOT_BE_CANCELLED("IDENTITY_403", "KYC cannot be cancelled in current status"),
	KYC_MANAGED_EXTERNALLY("IDENTITY_404", "KYC is managed by an external provider"),
	KYC_PROVIDER_NOT_CONFIGURED("IDENTITY_405", "KYC provider is not configured"),
	KYC_PROVIDER_ERROR("IDENTITY_406", "External KYC provider error"),
	KYC_WEBHOOK_SIGNATURE_INVALID("IDENTITY_407", "Invalid KYC webhook signature"),
	KYC_DOCUMENT_REQUIRED("IDENTITY_408", "KYC document is required"),
	KYC_DOCUMENT_NOT_FOUND("IDENTITY_409", "KYC document not found"),
	KYC_DOCUMENT_TYPE_INVALID("IDENTITY_410", "KYC document type is invalid"),

	AGENT_NOT_FOUND("IDENTITY_501", "Agent identity not found"),
	AGENT_CREATION_NOT_ALLOWED("IDENTITY_502", "User is not allowed to create agents"),

	CONSENT_NOT_FOUND("IDENTITY_601", "Consent record not found"),
	INVALID_IP_ADDRESS("IDENTITY_602", "Invalid IP address format"),

	FEEDBACK_NOT_FOUND("IDENTITY_701", "Feedback not found");

	private final String code;
	private final String defaultMessage;
}
