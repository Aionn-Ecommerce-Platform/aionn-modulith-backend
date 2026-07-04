package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.user.request.*;
import com.aionn.identity.adapter.rest.dto.user.response.*;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.user.UserDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.user.command.*;
import com.aionn.identity.application.dto.user.query.GetMyProfileQuery;
import com.aionn.identity.application.dto.user.view.*;
import com.aionn.identity.application.port.in.user.*;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@ExtendWith(MockitoExtension.class)
class UserControllerWebTest {

        @Mock
        private GetMyProfileInputPort getMyProfileInputPort;
        @Mock
        private VerifyEmailInputPort verifyEmailInputPort;
        @Mock
        private UpdateDisplayNameInputPort updateDisplayNameInputPort;
        @Mock
        private UpdateAvatarInputPort updateAvatarInputPort;
        @Mock
        private ChangeEmailInputPort changeEmailInputPort;
        @Mock
        private ChangePhoneInputPort changePhoneInputPort;
        @Mock
        private RequestAccountDeletionInputPort requestAccountDeletionInputPort;
        @Mock
        private CancelAccountDeletionInputPort cancelAccountDeletionInputPort;
        @Mock
        private RequestDataExportInputPort requestDataExportInputPort;
        @Mock
        private UserDtoMapper userDtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                UserController controller = new UserController(
                                getMyProfileInputPort, verifyEmailInputPort, updateDisplayNameInputPort,
                                updateAvatarInputPort, changeEmailInputPort, changePhoneInputPort,
                                requestAccountDeletionInputPort, cancelAccountDeletionInputPort,
                                requestDataExportInputPort, userDtoMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new IdentityExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(
                                                new ClientIpArgumentResolver(new ClientIpResolver()),
                                                new MockAuthenticationArgumentResolver())
                                .build();
        }

        @Test
        void getMyProfileReturnsUserProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                UserProfileView view = new UserProfileView("user-123", "alice@example.com", "0912345678",
                                "alice_smith", "alice_smith", "https://cdn.example.com/avatar.jpg",
                                Set.of("USER"), "ACTIVE", now, now, now);
                UserProfileResponse response = new UserProfileResponse("user-123", "alice@example.com", "0912345678",
                                "alice_smith", "alice_smith", "https://cdn.example.com/avatar.jpg",
                                Set.of("USER"), "ACTIVE", now, now, now);

                when(userDtoMapper.toGetMyProfileQuery("alice@example.com"))
                                .thenReturn(new GetMyProfileQuery("user-123"));
                when(getMyProfileInputPort.execute(any())).thenReturn(view);
                when(userDtoMapper.toProfileResponse(view)).thenReturn(response);

                mockMvc.perform(get("/api/v1/users/me")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.userId").value("user-123"))
                                .andExpect(jsonPath("$.data.username").value("alice_smith"))
                                .andExpect(jsonPath("$.data.email").value("alice@example.com"));

                verify(getMyProfileInputPort).execute(any());
        }

        @Test
        void sendVerifyEmailOtpSendsOtpToCurrentEmail() throws Exception {
                doNothing().when(verifyEmailInputPort).requestOtp(any());

                mockMvc.perform(post("/api/v1/users/me/verify-email/otp")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Verification OTP sent to email"));

                verify(verifyEmailInputPort).requestOtp(any());
        }

        @Test
        void confirmVerifyEmailOtpVerifiesEmail() throws Exception {
                doNothing().when(verifyEmailInputPort).confirm(any());

                mockMvc.perform(post("/api/v1/users/me/verify-email/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "123456"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Email verified"));

                verify(verifyEmailInputPort).confirm(any());
        }

        @Test
        void updateDisplayNameUpdatesAndReturnsProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                UserProfileView view = new UserProfileView("user-123", "alice@example.com", "0912345678",
                                "alice_jones", "alice_jones", null, Set.of("USER"), "ACTIVE", now, now, now);
                UserProfileResponse response = new UserProfileResponse("user-123", "alice@example.com", "0912345678",
                                "alice_jones", "alice_jones", null, Set.of("USER"), "ACTIVE", now, now, now);

                when(userDtoMapper.toUpdateDisplayNameCommand(eq("alice@example.com"),
                                any(ChangeDisplayNameRequest.class)))
                                .thenReturn(new UpdateDisplayNameCommand("user-123", "alice_jones"));
                when(updateDisplayNameInputPort.execute(any())).thenReturn(view);
                when(userDtoMapper.toProfileResponse(view)).thenReturn(response);

                mockMvc.perform(patch("/api/v1/users/me/display-name")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "displayName": "alice_jones"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.username").value("alice_jones"));

                verify(updateDisplayNameInputPort).execute(any());
        }

        @Test
        void updateAvatarUpdatesAndReturnsProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                UserProfileView view = new UserProfileView("user-123", "alice@example.com", "0912345678",
                                "alice_smith", "alice_smith", "https://cdn.example.com/new-avatar.jpg",
                                Set.of("USER"), "ACTIVE", now, now, now);
                UserProfileResponse response = new UserProfileResponse("user-123", "alice@example.com", "0912345678",
                                "alice_smith", "alice_smith", "https://cdn.example.com/new-avatar.jpg",
                                Set.of("USER"), "ACTIVE", now, now, now);

                when(userDtoMapper.toUpdateAvatarCommand(eq("alice@example.com"), any(ChangeAvatarRequest.class)))
                                .thenReturn(new UpdateAvatarCommand("user-123",
                                                "https://cdn.example.com/new-avatar.jpg"));
                when(updateAvatarInputPort.execute(any())).thenReturn(view);
                when(userDtoMapper.toProfileResponse(view)).thenReturn(response);

                mockMvc.perform(patch("/api/v1/users/me/avatar")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "avatarUrl": "https://cdn.example.com/new-avatar.jpg"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.avatarUrl")
                                                .value("https://cdn.example.com/new-avatar.jpg"));

                verify(updateAvatarInputPort).execute(any());
        }

        @Test
        void requestEmailChangeOtpSendsOtpToNewEmail() throws Exception {
                doNothing().when(changeEmailInputPort).requestOtp(any());

                mockMvc.perform(post("/api/v1/users/me/email-change/otp")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "newEmail": "alice.new@example.com"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("OTP sent to new email"));

                verify(changeEmailInputPort).requestOtp(any());
        }

        @Test
        void confirmEmailChangeUpdatesEmailAndReturnsProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                UserProfileView view = new UserProfileView("user-123", "alice.new@example.com", "0912345678",
                                "alice_smith", "alice_smith", null, Set.of("USER"), "ACTIVE", now, now, now);
                UserProfileResponse response = new UserProfileResponse("user-123", "alice.new@example.com",
                                "0912345678", "alice_smith", "alice_smith", null, Set.of("USER"), "ACTIVE", now, now,
                                now);

                when(changeEmailInputPort.confirm(any())).thenReturn(view);
                when(userDtoMapper.toProfileResponse(view)).thenReturn(response);

                mockMvc.perform(post("/api/v1/users/me/email-change/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "654321"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value("alice.new@example.com"));

                verify(changeEmailInputPort).confirm(any());
        }

        @Test
        void requestPhoneChangeOtpSendsOtpToNewPhone() throws Exception {
                doNothing().when(changePhoneInputPort).requestOtp(any());

                mockMvc.perform(post("/api/v1/users/me/phone-change/otp")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "newPhone": "0987654321"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("OTP sent to new phone"));

                verify(changePhoneInputPort).requestOtp(any());
        }

        @Test
        void confirmPhoneChangeUpdatesPhoneAndReturnsProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                UserProfileView view = new UserProfileView("user-123", "alice@example.com", "0987654321",
                                "alice_smith", "alice_smith", null, Set.of("USER"), "ACTIVE", now, now, now);
                UserProfileResponse response = new UserProfileResponse("user-123", "alice@example.com", "0987654321",
                                "alice_smith", "alice_smith", null, Set.of("USER"), "ACTIVE", now, now, now);

                when(changePhoneInputPort.confirm(any())).thenReturn(view);
                when(userDtoMapper.toProfileResponse(view)).thenReturn(response);

                mockMvc.perform(post("/api/v1/users/me/phone-change/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "111222"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.phone").value("0987654321"));

                verify(changePhoneInputPort).confirm(any());
        }

        @Test
        void requestAccountDeletionCreatesDeleteRequest() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime scheduledAt = now.plusDays(30);
                DeletionRequestView view = new DeletionRequestView("del-req-123", "PENDING", now, scheduledAt);
                DeletionRequestResponse response = new DeletionRequestResponse("del-req-123", "PENDING", now,
                                scheduledAt);

                when(userDtoMapper.toRequestAccountDeletionCommand("alice@example.com"))
                                .thenReturn(new RequestAccountDeletionCommand("user-123"));
                when(requestAccountDeletionInputPort.execute(any())).thenReturn(view);
                when(userDtoMapper.toDeletionRequestResponse(view)).thenReturn(response);

                mockMvc.perform(post("/api/v1/users/me/deletion-requests")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.requestId").value("del-req-123"))
                                .andExpect(jsonPath("$.data.status").value("PENDING"));

                verify(requestAccountDeletionInputPort).execute(any());
        }

        @Test
        void cancelAccountDeletionCancelsRequest() throws Exception {
                when(userDtoMapper.toCancelAccountDeletionCommand("alice@example.com"))
                                .thenReturn(new CancelAccountDeletionCommand("user-123"));
                doNothing().when(cancelAccountDeletionInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/users/me/deletion-requests")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Account deletion cancelled"));

                verify(cancelAccountDeletionInputPort).execute(any());
        }

        @Test
        void requestDataExportCreatesExportRequest() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                DataExportRequestView view = new DataExportRequestView("export-123", "PENDING", now);
                DataExportRequestResponse response = new DataExportRequestResponse("export-123", "PENDING", now);

                when(userDtoMapper.toRequestDataExportCommand("alice@example.com"))
                                .thenReturn(new RequestDataExportCommand("user-123"));
                when(requestDataExportInputPort.execute(any())).thenReturn(view);
                when(userDtoMapper.toDataExportResponse(view)).thenReturn(response);

                mockMvc.perform(post("/api/v1/users/me/data-exports")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.requestId").value("export-123"))
                                .andExpect(jsonPath("$.data.status").value("PENDING"));

                verify(requestDataExportInputPort).execute(any());
        }

        @Test
        void confirmVerifyEmailOtpRejectsBlankOtp() throws Exception {
                mockMvc.perform(post("/api/v1/users/me/verify-email/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

                verifyNoInteractions(verifyEmailInputPort);
        }

        @Test
        void confirmVerifyEmailOtpWhenOtpInvalidReturns400() throws Exception {
                doThrow(new IdentityException(IdentityErrorCode.OTP_INVALID))
                                .when(verifyEmailInputPort).confirm(any());

                mockMvc.perform(post("/api/v1/users/me/verify-email/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "999999"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_101"));
        }

        @Test
        void confirmVerifyEmailOtpWhenChallengeNotFoundReturns404() throws Exception {
                doThrow(new IdentityException(IdentityErrorCode.EMAIL_VERIFICATION_NOT_FOUND))
                                .when(verifyEmailInputPort).confirm(any());

                mockMvc.perform(post("/api/v1/users/me/verify-email/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "123456"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_217"));
        }

        @Test
        void updateDisplayNameRejectsBlankDisplayName() throws Exception {
                mockMvc.perform(patch("/api/v1/users/me/display-name")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "displayName": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

                verifyNoInteractions(updateDisplayNameInputPort);
        }

        @Test
        void updateDisplayNameRejectsTooShortDisplayName() throws Exception {
                mockMvc.perform(patch("/api/v1/users/me/display-name")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "displayName": "a"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

                verifyNoInteractions(updateDisplayNameInputPort);
        }

        @Test
        void updateAvatarWhenAvatarUrlInvalidReturns400() throws Exception {
                when(userDtoMapper.toUpdateAvatarCommand(eq("alice@example.com"), any(ChangeAvatarRequest.class)))
                                .thenReturn(new UpdateAvatarCommand("user-123", "ftp://bad.example.com/x"));
                when(updateAvatarInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.AVATAR_URL_INVALID));

                mockMvc.perform(patch("/api/v1/users/me/avatar")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "avatarUrl": "ftp://bad.example.com/x"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_213"));
        }

        @Test
        void requestEmailChangeOtpWhenEmailAlreadyExistsReturns409() throws Exception {
                doThrow(new IdentityException(IdentityErrorCode.EMAIL_ALREADY_EXISTS))
                                .when(changeEmailInputPort).requestOtp(any());

                mockMvc.perform(post("/api/v1/users/me/email-change/otp")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "newEmail": "taken@example.com"
                                                }
                                                """))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_002"));
        }

        @Test
        void requestPhoneChangeOtpWhenPhoneInvalidReturns400() throws Exception {
                doThrow(new IdentityException(IdentityErrorCode.PHONE_INVALID))
                                .when(changePhoneInputPort).requestOtp(any());

                mockMvc.perform(post("/api/v1/users/me/phone-change/otp")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "newPhone": "abc"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_006"));
        }

        @Test
        void confirmEmailChangeWhenChallengeNotFoundReturns404() throws Exception {
                when(changeEmailInputPort.confirm(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.EMAIL_CHANGE_NOT_FOUND));

                mockMvc.perform(post("/api/v1/users/me/email-change/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "111111"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_218"));
        }

        @Test
        void confirmPhoneChangeWhenChallengeNotFoundReturns404() throws Exception {
                when(changePhoneInputPort.confirm(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.PHONE_CHANGE_NOT_FOUND));

                mockMvc.perform(post("/api/v1/users/me/phone-change/confirm")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "otpCode": "222222"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_219"));
        }

        @Test
        void requestAccountDeletionWhenAlreadyRequestedReturns409() throws Exception {
                when(userDtoMapper.toRequestAccountDeletionCommand("alice@example.com"))
                                .thenReturn(new RequestAccountDeletionCommand("user-123"));
                when(requestAccountDeletionInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.ACCOUNT_DELETION_ALREADY_REQUESTED));

                mockMvc.perform(post("/api/v1/users/me/deletion-requests")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_214"));
        }

        @Test
        void cancelAccountDeletionWhenNoRequestExistsReturns404() throws Exception {
                when(userDtoMapper.toCancelAccountDeletionCommand("alice@example.com"))
                                .thenReturn(new CancelAccountDeletionCommand("user-123"));
                doThrow(new IdentityException(IdentityErrorCode.ACCOUNT_DELETION_NOT_FOUND))
                                .when(cancelAccountDeletionInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/users/me/deletion-requests")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_215"));
        }

        @Test
        void requestDataExportWhenAlreadyInProgressReturns409() throws Exception {
                when(userDtoMapper.toRequestDataExportCommand("alice@example.com"))
                                .thenReturn(new RequestDataExportCommand("user-123"));
                when(requestDataExportInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS));

                mockMvc.perform(post("/api/v1/users/me/data-exports")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_216"));
        }

        @Test
        void unauthorizedRequestReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/users/me"))
                                .andExpect(status().isUnauthorized());
        }
}
