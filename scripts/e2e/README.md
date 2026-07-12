# E2E API smoke test

Drives the identity + catalog REST API through a realistic user flow
(register → login → refresh → profile → address → consent → feedback →
catalog browsing → merchant registration → logout). Only exercises
endpoints backed by Postgres + Redis — no Sumsub, Google, Cloudinary,
Twilio, VNPay, or GHN calls.

## Run

```bash
# 1. Start Postgres + Redis (dev docker-compose or local install), then start
#    the app with the mock providers enabled. Minimum required env:

export IDENTITY_JWT_SECRET='dev-secret-at-least-32-characters-long-xxxx'

# Bypass Google reCAPTCHA — MockCaptchaTokenValidator accepts any non-empty token
export CAPTCHA_PROVIDER=mock

# Keep Twilio off so the OTP is returned in the response (dev only)
export TWILIO_ENABLED=false

# Optional: keep social login + media + KYC away from real providers
export IDENTITY_AUTH_GOOGLE_PROVIDER=mock
export IDENTITY_AUTH_FACEBOOK_PROVIDER=mock
export IDENTITY_MEDIA_PROVIDER=mock
export IDENTITY_KYC_PROVIDER=local

./gradlew :app:bootRun

# 2. In another terminal:
cd scripts/e2e
bash test-api-e2e.sh
```

## What it checks

- Every request returns the expected HTTP status. First unexpected status
  aborts the run with the body printed.
- The `timestamp` field in every response envelope ends with `Z` — this
  is the guard that `Instant` serialisation didn't regress back to
  `LocalDateTime`.
- Registration + auth flow produces working JWT + refresh tokens.
- Address CRUD works against the real geography seed.
- Merchant registration succeeds against a freshly-created user.

## Overrides

- `BASE_URL` — defaults to `http://localhost:8080`. Point at a staging
  host if you want.

## What it does NOT cover

Anything that would call out to a real external service, per the user's
current scope:

- `POST /api/v1/kyc/*` (Sumsub)
- `POST /api/v1/auth/social-login` (Google / Facebook)
- Media upload signature endpoints when `IDENTITY_MEDIA_PROVIDER=cloudinary`
- SMS / email verification endpoints
- Anything on the payment / shipping modules (not migrated yet)
