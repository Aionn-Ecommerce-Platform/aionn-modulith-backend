package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.method.request.CompleteStripeSetupIntentRequest;
import com.aionn.payment.adapter.rest.dto.method.request.LinkMethodRequest;
import com.aionn.payment.adapter.rest.dto.method.response.PaymentMethodResponse;
import com.aionn.payment.adapter.rest.dto.method.response.StripeSetupIntentResponse;
import com.aionn.payment.adapter.rest.dto.preference.request.UpdatePaymentPreferenceRequest;
import com.aionn.payment.adapter.rest.dto.preference.response.PaymentPreferenceResponse;
import com.aionn.payment.adapter.rest.mapper.method.PaymentMethodDtoMapper;
import com.aionn.payment.adapter.rest.mapper.preference.PaymentPreferenceDtoMapper;
import com.aionn.payment.adapter.rest.support.session.CurrentUserId;
import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.port.in.method.CompleteStripeSetupIntentInputPort;
import com.aionn.payment.application.port.in.method.CreateStripeSetupIntentInputPort;
import com.aionn.payment.application.port.in.method.LinkPaymentMethodInputPort;
import com.aionn.payment.application.port.in.method.ListPaymentMethodsInputPort;
import com.aionn.payment.application.port.in.method.RemovePaymentMethodInputPort;
import com.aionn.payment.application.port.in.method.VerifyPaymentMethodInputPort;
import com.aionn.payment.application.port.in.preference.GetPaymentPreferenceInputPort;
import com.aionn.payment.application.port.in.preference.UpdatePaymentPreferenceInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/methods")
@RequiredArgsConstructor
@Tag(name = "Payment - Method", description = "Stored payment method endpoints")
public class PaymentMethodController {

        private final LinkPaymentMethodInputPort linkPaymentMethodInputPort;
        private final VerifyPaymentMethodInputPort verifyPaymentMethodInputPort;
        private final RemovePaymentMethodInputPort removePaymentMethodInputPort;
        private final ListPaymentMethodsInputPort listPaymentMethodsInputPort;
        private final CreateStripeSetupIntentInputPort createStripeSetupIntentInputPort;
        private final CompleteStripeSetupIntentInputPort completeStripeSetupIntentInputPort;
        private final GetPaymentPreferenceInputPort getPaymentPreferenceInputPort;
        private final UpdatePaymentPreferenceInputPort updatePaymentPreferenceInputPort;
        private final PaymentMethodDtoMapper paymentMethodDtoMapper;
        private final PaymentPreferenceDtoMapper paymentPreferenceDtoMapper;

        @GetMapping("/preference")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get preferred payment option")
        public ResponseEntity<ApiResponse<PaymentPreferenceResponse>> preference(@CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentPreferenceDtoMapper.toResponse(getPaymentPreferenceInputPort.execute(userId)),
                                "Payment preference fetched"));
        }

        @PutMapping("/preference")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update preferred payment option")
        public ResponseEntity<ApiResponse<PaymentPreferenceResponse>> updatePreference(
                        @CurrentUserId String userId,
                        @Valid @RequestBody UpdatePaymentPreferenceRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentPreferenceDtoMapper.toResponse(
                                                updatePaymentPreferenceInputPort.execute(userId, request.paymentType(),
                                                                request.paymentMethodId())),
                                "Payment preference updated"));
        }

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Link method")
        public ResponseEntity<ApiResponse<PaymentMethodResponse>> link(
                        @CurrentUserId String userId,
                        @Valid @RequestBody LinkMethodRequest request) {
                LinkMethodCommand command = paymentMethodDtoMapper.toCommand(userId, request);
                return ApiResponse.createdResponse("Payment method linked",
                                paymentMethodDtoMapper.toResponse(linkPaymentMethodInputPort.execute(command)));
        }

        @PostMapping("/{methodId}/verify")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Verify method")
        public ResponseEntity<ApiResponse<PaymentMethodResponse>> verify(
                        @CurrentUserId String userId,
                        @PathVariable String methodId) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentMethodDtoMapper.toResponse(
                                                verifyPaymentMethodInputPort.execute(paymentMethodDtoMapper
                                                                .toVerifyCommand(userId, methodId))),
                                "Payment method verified"));
        }

        @PostMapping("/stripe/setup-intents")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Create Stripe card setup intent")
        public ResponseEntity<ApiResponse<StripeSetupIntentResponse>> createStripeSetupIntent(
                        @CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentMethodDtoMapper.toResponse(createStripeSetupIntentInputPort.execute(userId)),
                                "Stripe setup intent created"));
        }

        @PostMapping("/stripe/setup-intents/complete")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Complete Stripe card setup intent")
        public ResponseEntity<ApiResponse<PaymentMethodResponse>> completeStripeSetupIntent(
                        @CurrentUserId String userId,
                        @Valid @RequestBody CompleteStripeSetupIntentRequest request) {
                return ApiResponse.createdResponse("Payment method linked",
                                paymentMethodDtoMapper.toResponse(
                                                completeStripeSetupIntentInputPort.execute(userId,
                                                                request.setupIntentId())));
        }

        @DeleteMapping("/{methodId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Remove method")
        public ResponseEntity<Void> remove(
                        @CurrentUserId String userId,
                        @PathVariable String methodId) {
                removePaymentMethodInputPort.execute(paymentMethodDtoMapper.toRemoveCommand(userId, methodId));
                return ResponseEntity.noContent().build();
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List my methods")
        public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> listMine(@CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                paymentMethodDtoMapper.toResponses(listPaymentMethodsInputPort.execute(userId)),
                                "Methods fetched"));
        }
}
