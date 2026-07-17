# Running Rules

This document details the commands and configurations to manage infrastructure, start the application, run database migrations, and execute test suites.

---

## 1. Primary Recommendation: Use Makefile First (Preferred)

To ensure environment consistency and simplify multi-step processes (such as loading modular environment variables prior to compilation or Spring initialization), **always prioritize using the project's Makefile** over direct Gradle or shell executions.

The following operations must be done via `make` targets:

*   **Start Infrastructure Containers** (Postgres, Redis, OpenSearch):
    ```bash
    make infra-up
    ```
*   **Stop Infrastructure Containers**:
    ```bash
    make infra-down
    ```
*   **Restart Infrastructure**:
    ```bash
    make infra-restart
    ```
*   **Reset Databases & Clear Cache**:
    ```bash
    make reset-db
    ```
    *(Drops and cascades the public schema in Postgres, applies Flyway migrations from version 1.0, and flushes Redis cache).*
*   **Run Spring Boot Application Locally**:
    ```bash
    make run
    ```
    *(Loads all modular environment variables from `envs/*.env` before running Gradle bootRun so Spring Boot can resolve them).*
*   **Clean Build Directories**:
    ```bash
    make clean
    ```
*   **Run Unit & Integration Test Suites**:
    ```bash
    make test
    ```

---

## 2. Fallback: Local Gradle Executions (Manual)

If `make` is unavailable on the local system (e.g., Windows environment without GNU make installed), you may fallback to executing Gradle wrapper commands directly:

### Compile the Application
Ensure that MapStruct classes are generated before running tests:
```bash
./gradlew compileJava compileTestJava
# Or on Windows PowerShell:
.\gradlew.bat compileJava compileTestJava
```

### Run Module-Specific Tests
To run tests on a specific module (e.g., `inventory`):
```bash
./gradlew :modules:inventory:test
```

### Start App Server Manually (Requires Manual Env Loading)
Ensure that variables from `envs/` are set in your OS process before running:
```bash
./gradlew :app:bootRun
```

---

## 3. End-to-End (E2E) API Smoke Tests
To verify all APIs across modules work end-to-end against the running backend server:
```bash
# Run all E2E modules (identity, catalog, inventory)
powershell.exe -ExecutionPolicy Bypass -File scripts/run-e2e-suite.ps1 all

# Or run E2E for a specific module only
powershell.exe -ExecutionPolicy Bypass -File scripts/run-e2e-suite.ps1 inventory
```
*(The script will automatically start the background server with mocked third-party configurations like Twilio/Google Captcha, run the E2E scripts, and terminate the server cleanly afterwards).*
