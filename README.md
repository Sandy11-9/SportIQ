# SportIQ

SportIQ is a full-stack sports coaching application. The Angular frontend talks to a Spring Boot API backed by PostgreSQL; local Ollama models analyze uploaded videos and generate coaching feedback. Video processing uses FFmpeg/ffprobe.

## Stack

- Frontend: Angular 19, TypeScript, SCSS, Nginx
- Backend: Java 21 runtime, Spring Boot, Spring Security/JWT, Spring AI
- Database: PostgreSQL 16 with pgvector image
- Local AI: Ollama with `moondream` and `phi4-mini`
- Video processing: FFmpeg/ffprobe
- Tests: JUnit, Spring tests, Testcontainers, Angular/Karma

## Prerequisites

- Docker Desktop with Docker Compose v2
- At least 8 GB RAM available to Docker; more memory and disk space help with local AI models
- For real email verification: an SMTP account and app password

## Configure secrets

1. Copy `.env.example` to `.env` in the repository root.
2. Replace `SPORTIQ_JWT_SECRET` with a long random secret. In PowerShell, generate one with:

   ```powershell
   [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
   ```

3. Set `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` and `SPRING_MAIL_FROM` in `.env` for real email delivery. You can adjust `SPRING_MAIL_HOST` and `SPRING_MAIL_PORT` for your provider. Use a provider app password, not your normal account password.
4. Never commit `.env` or share its contents. If a credential was ever committed to Git, revoke/rotate it; deleting it from the latest file does not remove it from Git history.

## Run the full application

From the repository root:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f ollama-setup backend frontend
```

The first start downloads the AI models and can take a while. The setup container must finish with `MODELS_READY`, then the backend and frontend should start.

- Frontend: http://localhost:4200
- Backend API (direct): http://localhost:8080
- The frontend's Nginx server proxies `/api/` requests to the backend.

To stop:

```powershell
docker compose down
```

To preserve downloaded models and database data, keep the named volumes. To remove the app's data and downloaded models too, use `docker compose down -v`.

## Runtime and real AI smoke test

After startup, use PowerShell to confirm the UI returns HTTP 200 and the protected API returns HTTP 401 before logging in:

```powershell
(Invoke-WebRequest http://localhost:4200 -UseBasicParsing).StatusCode
try {
  (Invoke-WebRequest http://localhost:8080/api/sports -UseBasicParsing).StatusCode
} catch {
  [int]$_.Exception.Response.StatusCode
}
docker compose ps -a
```

Then verify the actual product flow in the browser:

1. Register with an email inbox you can access. Confirm the OTP message arrives from the SMTP provider, and enter the code.
2. Log in and open the sports/skills catalog.
3. Upload a playable MP4 between 5 and 20 seconds (maximum 50 MB) for a skill compatible with the clip.
4. Wait for analysis and confirm real model-generated scores, strengths, weaknesses and recommendations appear.
5. Open history, view the saved analysis, and refresh the page.
6. Open chatbot and diet-advice/calculator features.
7. Inspect `docker compose logs backend ollama` if processing fails.

An HTTP health response or mocked test alone does not validate model inference. A successful upload that runs through both Ollama models is required.

## Automated verification

The GitHub Actions workflow in `.github/workflows/runtime-verification.yml` runs on pushes and pull requests to `main`. It runs the backend Maven suite, production frontend build and Angular tests, then starts a Compose stack to verify container startup, HTTP routing and the Nginx API proxy. Its lightweight Compose override intentionally skips model downloads, so it does not prove real AI analysis.

The separate `.github/workflows/full-e2e.yml` workflow is an expensive, real-model test. It downloads `moondream` and `phi4-mini`, starts the actual Dockerized application, routes the application SMTP client to a temporary Mailpit SMTP inbox, retrieves the verification OTP from that inbox, verifies a new account, logs in, uploads the repository's demo video, and checks the returned analysis and history. This proves the app sends and receives the OTP through an SMTP server and completes a real Ollama analysis. Mailpit is a test inbox; this workflow does **not** prove delivery through your personal Gmail/SMTP provider. To verify external delivery, use the real SMTP settings in `.env` and confirm the OTP lands in an inbox you control.

## Local tests

Backend tests require Java 21, Maven, FFmpeg and Docker for PostgreSQL Testcontainers:

```powershell
cd backend
mvn test
```

Frontend build and unit tests:

```powershell
cd frontend
npm ci
npm run build
npm test -- --watch=false --browsers=ChromeHeadlessCI
```

## Important security note

Never put SMTP passwords, production JWT secrets, or other credentials in tracked configuration. Configure secrets through environment variables. If the previously committed SMTP credential is genuine, revoke it with the email provider immediately; the current configuration no longer contains it, but old Git history may still.
