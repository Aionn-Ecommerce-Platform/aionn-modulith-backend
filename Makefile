CONTAINER ?= docker
COMPOSE_FILE := docker/docker-compose.yml
ENV_FILE := envs/common.env
COMPOSE := $(CONTAINER) compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE)

.PHONY: build test smoke run clean infra-up infra-down infra-restart infra-logs infra-ps infra-config reset-db

build:
	./gradlew build -x test

test:
	./gradlew test

smoke:
	./gradlew :app:test

run:
	./gradlew :app:bootRun

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
	$(CONTAINER) exec aionn-modulith-postgres psql -U postgres -d aionn -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
	@echo "Flushing Redis cache..."
	$(CONTAINER) exec aionn-modulith-redis redis-cli -a hello FLUSHALL
