-- -----------------------------------------------------------------------------
-- Squashed from V9.0__init_notification_schema.sql
-- -----------------------------------------------------------------------------
CREATE TABLE notification_templates (
    template_id  VARCHAR(50) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    channel      VARCHAR(20)  NOT NULL,
    category     VARCHAR(20)  NOT NULL,
    locale       VARCHAR(20)  NOT NULL DEFAULT 'vi-VN',
    subject      TEXT,
    content      TEXT NOT NULL,
    placeholders JSONB NOT NULL DEFAULT '[]'::jsonb,
    version      INT  NOT NULL DEFAULT 1,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_notification_templates_event_channel_locale
    ON notification_templates(event_type, channel, locale);

CREATE TABLE notifications (
    noti_id              VARCHAR(50) PRIMARY KEY,
    user_id              VARCHAR(50) NOT NULL,
    template_id          VARCHAR(50),
    channel              VARCHAR(20) NOT NULL,
    category             VARCHAR(20) NOT NULL,
    priority             VARCHAR(20) NOT NULL,
    subject              TEXT,
    content              TEXT NOT NULL,
    campaign_id          VARCHAR(50),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count          INT NOT NULL DEFAULT 0,
    last_failure_reason  TEXT,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at              TIMESTAMPTZ,
    read_at              TIMESTAMPTZ,
    deleted_at           TIMESTAMPTZ
);
CREATE INDEX idx_notifications_user_status     ON notifications(user_id, status);
CREATE INDEX idx_notifications_campaign_status ON notifications(campaign_id, status);
CREATE INDEX idx_notifications_status_retry    ON notifications(status, retry_count);

CREATE TABLE device_tokens (
    token_id      VARCHAR(50) PRIMARY KEY,
    user_id       VARCHAR(50) NOT NULL,
    device_token  VARCHAR(512) NOT NULL,
    os            VARCHAR(20),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    version       BIGINT      NOT NULL DEFAULT 0,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_device_tokens_user              ON device_tokens(user_id);
CREATE UNIQUE INDEX idx_device_tokens_user_token ON device_tokens(user_id, device_token);

CREATE TABLE notification_subscriptions (
    user_id    VARCHAR(50) PRIMARY KEY,
    settings   JSONB NOT NULL DEFAULT '{}'::jsonb,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_configurations (
    provider_id           VARCHAR(50) PRIMARY KEY,
    channel               VARCHAR(20) NOT NULL,
    provider_type         VARCHAR(50) NOT NULL,
    config                JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    configured_by         VARCHAR(50),
    version               BIGINT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_provider_channel_active ON notification_configurations(channel, is_active);

-- ---------------------------------------------------------------------
-- Default identity-driven templates (vi-VN). Event-type strings match
-- IdentityNotificationAdapter and IdentityEventListener.
-- ---------------------------------------------------------------------
INSERT INTO notification_templates (template_id, event_type, channel, category, locale, subject, content, placeholders, version, is_active)
VALUES
('idn_pwd_reset_em_vi',
 'identity.password-reset-requested', 'EMAIL', 'SECURITY', 'vi-VN',
 'Yeu cau dat lai mat khau',
 'Ban vua yeu cau dat lai mat khau. Token: {{resetToken}}. Neu khong phai ban, vui long bo qua email nay.',
 '["resetToken"]'::jsonb, 1, TRUE),

('idn_pwd_changed_em_vi',
 'identity.password-changed', 'EMAIL', 'SECURITY', 'vi-VN',
 'Mat khau da duoc thay doi',
 'Mat khau tai khoan Aionn cua ban vua duoc thay doi (kenh: {{channelHint}}). Neu khong phai ban, hay lien he ho tro ngay.',
 '["channelHint"]'::jsonb, 1, TRUE),

('idn_pwd_changed_in_vi',
 'identity.password-changed', 'IN_APP', 'SECURITY', 'vi-VN',
 'Mat khau da duoc thay doi',
 'Mat khau cua ban vua duoc thay doi qua {{channelHint}}.',
 '["channelHint"]'::jsonb, 1, TRUE),

('idn_email_changed_em_vi',
 'identity.email-changed', 'EMAIL', 'SECURITY', 'vi-VN',
 'Email da duoc cap nhat',
 'Email tai khoan Aionn vua doi tu {{oldEmail}} sang {{newEmail}}. Neu khong phai ban, hay lien he ho tro ngay.',
 '["oldEmail","newEmail"]'::jsonb, 1, TRUE),

('idn_email_changed_in_vi',
 'identity.email-changed', 'IN_APP', 'SECURITY', 'vi-VN',
 'Email da duoc cap nhat',
 'Email tai khoan vua doi sang {{newEmail}}.',
 '["newEmail"]'::jsonb, 1, TRUE),

('idn_phone_changed_sms_vi',
 'identity.phone-changed', 'SMS', 'SECURITY', 'vi-VN',
 NULL,
 'Aionn: so dien thoai tai khoan vua doi tu {{oldPhone}} sang {{newPhone}}.',
 '["oldPhone","newPhone"]'::jsonb, 1, TRUE),

('idn_phone_changed_in_vi',
 'identity.phone-changed', 'IN_APP', 'SECURITY', 'vi-VN',
 'So dien thoai da thay doi',
 'So dien thoai tai khoan vua doi sang {{newPhone}}.',
 '["newPhone"]'::jsonb, 1, TRUE),

('idn_email_otp_em_vi',
 'identity.email-otp', 'EMAIL', 'SECURITY', 'vi-VN',
 'Ma xac thuc Aionn',
 'Ma xac thuc email cua ban la {{otpCode}}. Hieu luc 5 phut. Vui long khong chia se voi nguoi khac.',
 '["otpCode"]'::jsonb, 1, TRUE),

('idn_phone_otp_sms_vi',
 'identity.phone-otp', 'SMS', 'SECURITY', 'vi-VN',
 NULL,
 'Aionn: ma xac thuc cua ban la {{otpCode}}. Hieu luc 5 phut. Khong chia se voi ai.',
 '["otpCode"]'::jsonb, 1, TRUE),

('idn_reg_otp_sms_vi',
 'identity.registration-otp', 'SMS', 'SECURITY', 'vi-VN',
 NULL,
 'Aionn: ma OTP dang ky cua ban la {{otpCode}}. Hieu luc 5 phut. Khong chia se voi ai.',
 '["otpCode"]'::jsonb, 1, TRUE);


-- ---------------------------------------------------------------------
-- Chat -> notification fan-out templates. Triggered by
-- MessageSentIntegrationEvent for offline recipients only.
-- ---------------------------------------------------------------------
INSERT INTO notification_templates (template_id, event_type, channel, category, locale, subject, content, placeholders, version, is_active)
VALUES
('chat_msg_in_app_vi',
 'chat.message-received', 'IN_APP', 'CHAT', 'vi-VN',
 '{{senderName}} vừa gửi tin nhắn',
 '{{messagePreview}}',
 '["senderName","messagePreview","conversationId"]'::jsonb, 1, TRUE),

('chat_msg_email_vi',
 'chat.message-received', 'EMAIL', 'CHAT', 'vi-VN',
 'Tin nhắn mới từ {{senderName}}',
 '{{senderName}} đã gửi cho bạn một tin nhắn:\n\n{{messagePreview}}\n\nMở Aionn để trả lời.',
 '["senderName","messagePreview","conversationId"]'::jsonb, 1, TRUE),

('chat_msg_push_vi',
 'chat.message-received', 'PUSH', 'CHAT', 'vi-VN',
 '{{senderName}}',
 '{{messagePreview}}',
 '["senderName","messagePreview","conversationId"]'::jsonb, 1, TRUE);

-- -----------------------------------------------------------------------------
-- Squashed from V9.1__add_english_notification_templates.sql
-- -----------------------------------------------------------------------------
-- =====================================================================
-- Seed translation data for English (en-US) templates
-- =====================================================================

INSERT INTO notification_templates (template_id, event_type, channel, category, locale, subject, content, placeholders, version, is_active)
VALUES
('idn_pwd_reset_em_en',
 'identity.password-reset-requested', 'EMAIL', 'SECURITY', 'en-US',
 'Password Reset Request',
 'You have requested a password reset. Token: {{resetToken}}. If this was not you, please ignore this email.',
 '["resetToken"]'::jsonb, 1, TRUE),

('idn_pwd_changed_em_en',
 'identity.password-changed', 'EMAIL', 'SECURITY', 'en-US',
 'Your password has been changed',
 'Your Aionn account password has been changed (channel: {{channelHint}}). If this was not you, please contact support immediately.',
 '["channelHint"]'::jsonb, 1, TRUE),

('idn_pwd_changed_in_en',
 'identity.password-changed', 'IN_APP', 'SECURITY', 'en-US',
 'Password Changed',
 'Your password has been changed via {{channelHint}}.',
 '["channelHint"]'::jsonb, 1, TRUE),

('idn_email_changed_em_en',
 'identity.email-changed', 'EMAIL', 'SECURITY', 'en-US',
 'Email Address Updated',
 'Your Aionn account email address was changed from {{oldEmail}} to {{newEmail}}. If this was not you, please contact support immediately.',
 '["oldEmail","newEmail"]'::jsonb, 1, TRUE),

('idn_email_changed_in_en',
 'identity.email-changed', 'IN_APP', 'SECURITY', 'en-US',
 'Email Address Updated',
 'Your account email address has been updated to {{newEmail}}.',
 '["newEmail"]'::jsonb, 1, TRUE),

('idn_phone_changed_sms_en',
 'identity.phone-changed', 'SMS', 'SECURITY', 'en-US',
 NULL,
 'Aionn: your account phone number has been changed from {{oldPhone}} to {{newPhone}}.',
 '["oldPhone","newPhone"]'::jsonb, 1, TRUE),

('idn_phone_changed_in_en',
 'identity.phone-changed', 'IN_APP', 'SECURITY', 'en-US',
 'Phone Number Updated',
 'Your account phone number has been updated to {{newPhone}}.',
 '["newPhone"]'::jsonb, 1, TRUE),

('idn_email_otp_em_en',
 'identity.email-otp', 'EMAIL', 'SECURITY', 'en-US',
 'Aionn Verification Code',
 'Your email verification code is {{otpCode}}. Valid for 5 minutes. Please do not share it with others.',
 '["otpCode"]'::jsonb, 1, TRUE),

('idn_phone_otp_sms_en',
 'identity.phone-otp', 'SMS', 'SECURITY', 'en-US',
 NULL,
 'Aionn: your verification code is {{otpCode}}. Valid for 5 minutes. Do not share it.',
 '["otpCode"]'::jsonb, 1, TRUE),

('idn_reg_otp_sms_en',
 'identity.registration-otp', 'SMS', 'SECURITY', 'en-US',
 NULL,
 'Aionn: your registration OTP is {{otpCode}}. Valid for 5 minutes. Do not share it.',
 '["otpCode"]'::jsonb, 1, TRUE),

('chat_msg_in_app_en',
 'chat.message-received', 'IN_APP', 'CHAT', 'en-US',
 '{{senderName}} sent a message',
 '{{messagePreview}}',
 '["senderName","messagePreview","conversationId"]'::jsonb, 1, TRUE),

('chat_msg_email_en',
 'chat.message-received', 'EMAIL', 'CHAT', 'en-US',
 'New message from {{senderName}}',
 '{{senderName}} has sent you a message:\n\n{{messagePreview}}\n\nOpen Aionn to reply.',
 '["senderName","messagePreview","conversationId"]'::jsonb, 1, TRUE),

('chat_msg_push_en',
 'chat.message-received', 'PUSH', 'CHAT', 'en-US',
 '{{senderName}}',
 '{{messagePreview}}',
 '["senderName","messagePreview","conversationId"]'::jsonb, 1, TRUE);

-- -----------------------------------------------------------------------------
-- Squashed from V9.2__seed_user_notifications.sql
-- -----------------------------------------------------------------------------
-- Seed some mock notifications for default buyer_001 to show off on the front-end header popover
INSERT INTO notifications (
    noti_id, user_id, template_id, channel, category, priority,
    subject, content, campaign_id, status, retry_count,
    created_at, updated_at, sent_at, read_at
) VALUES
(
    'not_00000000000000000000000001', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'ORDER', 'HIGH',
    'Giao kiện hàng thành công', 'Kiện hàng SPXVN065617513426 của đơn hàng 2606130F61PQFS đã giao thành công đến bạn.',
    NULL, 'SENT', 0, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NULL
),
(
    'not_00000000000000000000000002', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'HIGH',
    'SIÊU SALE BÁCH HÓA GIẢM ĐẾN 50%', '❤️ Áp thêm mã giảm 500K sẵn trong ví 💚 Mã giảm ngành hàng đến 300K, 150K 💛 Cùng mã miễn phí vận chuyển tận nơi 💙 Nhiều tầng giảm giá - Chốt đơn ngay nha!',
    NULL, 'SENT', 0, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NULL
),
(
    'not_00000000000000000000000003', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'MEDIUM',
    'SĂN VOUCHER XTRA ĐẾN 6 TRIỆU NGAY', 'ðŸŒŸ Cùng mã giảm ngành hàng đến 3 Triệu 💟 Thêm mã giảm 666K, 500K tràn ngập ðŸ“º Săn deal giảm 50% tại Shopee Live 💥 0h sale khủng - Mua sạch giỏ hàng!',
    NULL, 'SENT', 0, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '4 hours'
),
(
    'not_00000000000000000000000004', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'PROMOTION', 'MEDIUM',
    'Thời gian sắp hết!', 'ðŸ—“️ Voucher của bạn sẽ hết hạn vào ngày mai. Mua ngay để tiết kiệm hơn!',
    NULL, 'SENT', 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NULL
),
(
    'not_00000000000000000000000005', '01KV05RTC7NA4KZMMP8KXX7461', NULL, 'IN_APP', 'ORDER', 'HIGH',
    'Giao kiện hàng thành công', 'Kiện hàng SPXVN064677750536 của đơn hàng 2606130G5T9EBC đã giao thành công đến bạn.',
    NULL, 'SENT', 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'
);
