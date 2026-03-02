# ── Makefile — Developer Shortcuts ──
# Usage: make <target>   |   make help

# Load .env variables (DB credentials) for all targets.
# ≈ dotenv in Node.js — makes POSTGRES_USER etc. available to subprocesses.
include .env
export

# ── Local Development (one terminal per target) ──

.PHONY: db be fe

db:            ## Start PostgreSQL container
	docker compose up postgres

be:            ## Start backend (Spring Boot, dev profile)
	cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

fe:            ## Start frontend (Vite dev server)
	cd frontend && pnpm install && pnpm dev

# ── Code Quality Checks (≈ CI locally) ──

.PHONY: be-check fe-check check

be-check:      ## Backend: Spotless + Checkstyle + Tests
	cd backend && ./mvnw verify -B

fe-check:      ## Frontend: lint + test
	cd frontend && pnpm lint && pnpm test

check: be-check fe-check  ## Run all checks (backend + frontend)

# ── Local Stack (docker-compose.yml — no TLS) ──

.PHONY: up down logs

up:            ## Start all services locally (no TLS)
	docker compose up --build

down:          ## Stop local services
	docker compose down

logs:          ## Follow logs (local)
	docker compose logs -f

# ── Production (docker-compose.prod.yml — HTTPS on VPS) ──

.PHONY: prod-up prod-down prod-logs cert-init cert-down

prod-up:       ## Start production stack (HTTPS)
	docker compose -f docker-compose.prod.yml up --build -d

prod-down:     ## Stop production stack
	docker compose -f docker-compose.prod.yml down

prod-logs:     ## Follow production logs
	docker compose -f docker-compose.prod.yml logs -f

cert-init:     ## Bootstrap initial Let's Encrypt certificate
	docker compose -f docker-compose.initial-cert.yml up -d

cert-down:     ## Stop cert-init stack
	docker compose -f docker-compose.initial-cert.yml down

# ── Utilities ──

.PHONY: db-shell help

db-shell:      ## Open psql shell in running PostgreSQL container
	docker compose exec postgres psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

help:          ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*##' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
