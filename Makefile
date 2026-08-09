include envs/common.env

SHELL := bash

CONTAINER ?= docker
COMPOSE_FILE := docker/docker-compose.yml
ENV_FILE := envs/common.env
PROJECT := aionn-modulith-backend
COMPOSE := $(CONTAINER) compose -p $(PROJECT) -f $(COMPOSE_FILE) --env-file $(ENV_FILE)

LOAD_ENV := set -a; . envs/common.env; . envs/identity.env; . envs/catalog.env; . envs/inventory.env; . envs/ordering.env; . envs/payment.env; . envs/shipping.env; . envs/promotion.env; . envs/notification.env; . envs/chat.env; set +a

.PHONY: build test smoke e2e image-build image-run clean infra-up infra-down infra-restart infra-logs infra-ps infra-config reset-db

build:
	./gradlew build -x test

test:
	./gradlew test

smoke:
	./gradlew :app:test

e2e:
	powershell -ExecutionPolicy Bypass -File scripts/run-e2e-suite.ps1 -Module all

image-build:
	$(CONTAINER) build --tag aionn-modulith-backend:local .

image-run:
	$(CONTAINER) run --rm --name aionn-modulith-backend-local \
		--add-host host.docker.internal:host-gateway \
		--env-file envs/common.env \
		--env-file envs/identity.env \
		--env-file envs/catalog.env \
		--env-file envs/inventory.env \
		--env-file envs/ordering.env \
		--env-file envs/payment.env \
		--env-file envs/shipping.env \
		--env-file envs/promotion.env \
		--env-file envs/notification.env \
		--env-file envs/chat.env \
		-e POSTGRES_HOST=host.docker.internal \
		-e REDIS_HOST=host.docker.internal \
		-e CATALOG_SEARCH_OPENSEARCH_HOST=host.docker.internal \
		-e CAPTCHA_PROVIDER=mock \
		-e CAPTCHA_EXPECTED_TOKEN= \
		-e TWILIO_ENABLED=false \
		-e IDENTITY_AUTH_GOOGLE_PROVIDER=mock \
		-e IDENTITY_AUTH_FACEBOOK_PROVIDER=mock \
		-e IDENTITY_MEDIA_PROVIDER=mock \
		-e IDENTITY_KYC_PROVIDER=local \
		-p 8080:8080 aionn-modulith-backend:local

run:
	$(LOAD_ENV); ./gradlew :app:bootRun

clean:
	./gradlew clean

infra-up:
	$(COMPOSE) up -d

infra-down:
	$(COMPOSE) down

infra-restart:
	$(COMPOSE) down
	$(COMPOSE) up -d

infra-logs:
	$(COMPOSE) logs -f

infra-ps:
	$(COMPOSE) ps

infra-config:
	$(COMPOSE) config

reset-db:
	@echo "Resetting Postgres database schema..."
	$(CONTAINER) exec aionn-modulith-postgres psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
	@echo "Flushing Redis cache..."
	$(CONTAINER) exec aionn-modulith-redis redis-cli -a "$(REDIS_PASSWORD)" FLUSHALL
