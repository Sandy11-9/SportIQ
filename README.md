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
- For registration/email verification: an SMTP account and an app password (optional until you test sign-up)

## Configure secrets

1. Copy `.env.example` to `.env` in the repository root.
2. Replace `SPORTIQ_JWT_SECRET` with a long random secret. In PowerShell, generate one with:

   ```powershell
   [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
   ```

3. For email verification, set `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` and `SPRING_MAIL_FROM` in `.env`. Use a provider app password, not your normal account password.
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

## Runtime smoke test

After startup, run in PowerShell:

```powershell
# The UI should return HTTP 200.
(Invoke-WebRequest http://localhost:4200 -UseBasicParsing).StatusCode

# This protected endpoint should respond with HTTP 401 when no token is supplied.
try {
  (Invoke-WebRequest http://localhost:8080/api/sports -UseBasicParsing).StatusCode
} catch {
  [int]$_.Exception.Response.StatusCode
}

# All core containers should be running; ollama-setup should have exited successfully.
docker compose ps -a
```

Then verify an actual user flow in the browser:

1. Register using an email inbox you can access and confirm the OTP arrives.
2. Log in and open the sports/skills catalog.
3. Upload a playable MP4 video between 5 and 20 seconds (maximum 50 MB) for one of the skills listed by the app.
4. Wait for analysis and verify scores, strengths, weaknesses and recommendations appear.
5. Open history, view the saved analysis, and refresh the page.
6. Open the chatbot and diet-advice/calculator features.
7. Run the upload again and inspect `docker compose logs backend ollama` if processing fails.

An HTTP response alone does not validate AI quality; the successful upload and analysis flow is required for that. Test with a video you have permission to use.

## Automated verification

The GitHub Actions workflow in `.github/workflows/runtime-verification.yml` runs on pushes and pull requests to `main`. It runs backend Maven tests (including PostgreSQL-backed tests), builds the production frontend, runs Angular unit tests, and launches the Docker Compose stack to verify that the UI returns HTTP 200 and that the protected backend/API proxy return the expected HTTP 401 for anonymous calls. The CI Compose override deliberately skips downloading the AI models, so the CI runtime smoke validates container startup and routing, not real-model inference.

A real video-analysis run still needs to be performed locally with the models downloaded. The backend end-to-end tests mock Ollama calls, so they validate API integration but do not establish AI analysis quality.

Locally, backend tests require Java 21, Maven and FFmpeg; PostgreSQL integration tests use Testcontainers and therefore need Docker. Frontend commands:

```powershell
cd frontend
npm ci
npm run build
npm test -- --watch=false --browsers=ChromeHeadlessCI
```

Backend commands:

```powershell
cd backend
mvn test
```

## Important security note

Never put SMTP passwords, production JWT secrets, or other credentials in tracked configuration. Configure secrets through environment variables. If the previously committed SMTP credential is genuine, revoke it with the email provider immediately and replace it in your local `.env`.
