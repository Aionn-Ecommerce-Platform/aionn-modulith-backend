package com.aionn.identity.infrastructure.persistence.adapter.account;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.port.out.user.DataExportPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.DataExportStatus;
import com.aionn.identity.infrastructure.persistence.entity.DataExportRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.DataExportRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataExportPersistenceAdapter implements DataExportPort {

    /**
     * A request is only abandoned after this long. The worker polls continuously, so a row older than
     * this is a stuck deployment rather than a queue backlog.
     */
    private static final Duration STALE_REQUEST_AFTER = Duration.ofHours(1);
    private static final String CONTENT_TYPE = "application/json";

    private final DataExportRequestRepository dataExportRequestRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Registers the request and returns immediately. The payload is built by the export worker, so the
     * caller is not held open while nine tables are read and serialised.
     */
    @Override
    @Transactional
    public DataExportRequestView save(String userId) {
        // Serialize concurrent requests for the same user via the user row lock so two active rows
        // can never be inserted at once.
        UserEntity user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        Instant now = clock.instant();
        dataExportRequestRepository.failStaleRequests(
                userId,
                DataExportStatus.FAILED,
                List.of(DataExportStatus.REQUESTED, DataExportStatus.PROCESSING),
                now.minus(STALE_REQUEST_AFTER),
                now);

        boolean active = dataExportRequestRepository.existsByUser_UserIdAndStatusIn(
                userId,
                List.of(DataExportStatus.REQUESTED, DataExportStatus.PROCESSING));
        if (active) {
            throw new IdentityException(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS);
        }

        DataExportRequestEntity request = DataExportRequestEntity.builder()
                .exportRequestId(IdGenerator.ulid())
                .user(user)
                .status(DataExportStatus.REQUESTED)
                .requestedAt(now)
                .build();
        DataExportRequestEntity saved = dataExportRequestRepository.save(request);
        return toView(saved);
    }

    @Override
    public boolean hasActiveRequest(String userId) {
        return dataExportRequestRepository.existsByUser_UserIdAndStatusIn(
                userId,
                List.of(DataExportStatus.REQUESTED, DataExportStatus.PROCESSING));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataExportPayload> findPayload(String requestId, String userId) {
        return dataExportRequestRepository.findById(requestId)
                .filter(entity -> entity.getUser().getUserId().equals(userId))
                .filter(entity -> entity.getStatus() == DataExportStatus.COMPLETED)
                .filter(entity -> entity.getFileUrl() != null && !entity.getFileUrl().isBlank())
                .map(entity -> new DataExportPayload(
                        entity.getExportRequestId(), CONTENT_TYPE, entity.getFileUrl(),
                        entity.getCompletedAt()));
    }

    @Override
    public List<PendingExport> claimPending(int limit) {
        return jdbcTemplate.query("""
                UPDATE data_export_requests
                   SET status = 'PROCESSING'
                 WHERE export_request_id IN (
                       SELECT export_request_id
                         FROM data_export_requests
                        WHERE status = 'REQUESTED'
                        ORDER BY requested_at
                        LIMIT ?
                          FOR UPDATE SKIP LOCKED
                       )
                RETURNING export_request_id, user_id
                """,
                (rs, rowNum) -> new PendingExport(rs.getString(1), rs.getString(2)),
                Math.max(1, limit));
    }

    @Override
    public void complete(String requestId, String payload) {
        jdbcTemplate.update("""
                UPDATE data_export_requests
                   SET status = 'COMPLETED', file_url = ?, completed_at = ?
                 WHERE export_request_id = ? AND status = 'PROCESSING'
                """,
                payload,
                Timestamp.from(clock.instant()),
                requestId);
    }

    @Override
    public void fail(String requestId) {
        jdbcTemplate.update("""
                UPDATE data_export_requests
                   SET status = 'FAILED', completed_at = ?
                 WHERE export_request_id = ? AND status = 'PROCESSING'
                """,
                Timestamp.from(clock.instant()),
                requestId);
    }

    @Override
    public String buildPayload(String userId) {
        return toJson(snapshot(userId));
    }

    /**
     * Synchronous snapshot of the identity-owned data. It deliberately excludes credentials
     * (password hash, MFA secret, refresh tokens) and provider tokens.
     */
    private Map<String, Object> snapshot(String userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", first(query("""
                SELECT user_id, email, phone, username, display_name, avatar_url, status,
                       email_verified_at, phone_verified_at, created_at, updated_at
                  FROM users WHERE user_id = ?
                """, userId)));
        data.put("roles", query("SELECT role FROM user_roles WHERE user_id = ?", userId));
        data.put("addresses", query("""
                SELECT address_id, contact_name, phone, province_code, province_name,
                       district_code, district_name, ward_code, ward_name, detail_address,
                       full_address, address_type, is_default, created_at, updated_at
                  FROM user_addresses WHERE user_id = ? ORDER BY created_at DESC
                """, userId));
        data.put("consents", query("""
                SELECT consent_type, version, agreed_at, revoked_at, ip_address
                  FROM user_consents WHERE user_id = ? ORDER BY agreed_at DESC
                """, userId));
        data.put("preferences", first(query("""
                SELECT language, currency, timezone, theme,
                       notification_settings::text AS notification_settings,
                       ai_privacy_settings::text AS ai_privacy_settings, updated_at
                  FROM user_preferences WHERE user_id = ?
                """, userId)));
        data.put("feedback", query("""
                SELECT feedback_id, category, subject, content, rating, contact_email,
                       contact_phone, status, handled_at, created_at, updated_at
                  FROM user_feedbacks WHERE user_id = ? ORDER BY created_at DESC
                """, userId));
        data.put("kycProfiles", query("""
                SELECT kyc_id, doc_type, status, provider, provider_level_name,
                       provider_review_status, submitted_at, approved_at, reject_reason, created_at
                  FROM kyc_profiles WHERE user_id = ? ORDER BY created_at DESC
                """, userId));
        data.put("socialAccounts", query(
                "SELECT provider, created_at FROM social_accounts WHERE user_id = ?", userId));
        data.put("agents", query("""
                SELECT agent_id, name, permissions::text AS permissions, status, expiry_at, created_at
                  FROM agent_identities WHERE owner_id = ?
                """, userId));
        data.put("sessions", query("""
                SELECT session_id, status, created_at, last_active_at, expires_at
                  FROM auth_sessions WHERE user_id = ?
                """, userId));
        return data;
    }

    private List<Map<String, Object>> query(String sql, String userId) {
        return jdbcTemplate.queryForList(sql, userId);
    }

    private static Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private String toJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize data export", ex);
        }
    }

    private static DataExportRequestView toView(DataExportRequestEntity saved) {
        return new DataExportRequestView(
                saved.getExportRequestId(),
                saved.getStatus().name(),
                saved.getRequestedAt());
    }
}
