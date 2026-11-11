SHELL := bash

CONTAINER ?= docker
COMPOSE_FILE := docker/docker-compose.yml
ENV_FILE := envs/common.env
PROJECT := aionn-modulith-backend
COMPOSE := $(CONTAINER) compose -p $(PROJECT) -f $(COMPOSE_FILE) --env-file $(ENV_FILE)
ENV_FILES := $(ENV_FILE) $(filter-out $(ENV_FILE),$(wildcard envs/*.env))
MODULE_ENV_FILES := $(filter-out $(ENV_FILE),$(ENV_FILES))
LOAD_ENV := set -a; . $(ENV_FILE); $(foreach f,$(MODULE_ENV_FILES),. $(f);) set +a

.PHONY: build test smoke e2e image-build image-run run clean infra-up infra-down infra-restart infra-logs infra-ps infra-config reset-db

build:
	./gradlew build -x test

test:
	./gradlew test

smoke:
	./gradlew :app:test

e2e:
	powershell -ExecutionPolicy Bypass -File scripts/e2e/run-e2e-suite.ps1 -Module all

image-build:
	$(CONTAINER) build --tag aionn-modulith-backend:local .

image-run:
	$(CONTAINER) run --rm --name aionn-modulith-backend-local \
		--add-host host.docker.internal:host-gateway \
		$(foreach f,$(ENV_FILES),--env-file $(f) )\
		-e POSTGRES_HOST=host.docker.internal \
		-e REDIS_HOST=host.docker.internal \
		-e CATALOG_SEARCH_OPENSEARCH_HOST=host.docker.internal \
		-p 127.0.0.1:8080:8080 aionn-modulith-backend:local

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
	$(COMPOSE) exec -T postgres sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"'
	@echo "Flushing Redis cache..."
	$(COMPOSE) exec -T redis sh -c 'redis-cli --no-auth-warning -a "$$REDIS_PASSWORD" FLUSHALL'
