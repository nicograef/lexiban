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

# ── Production ──

.PHONY: up down logs

up:            ## Start all services (production build)
	docker compose up --build

down:          ## Stop all services
	docker compose down

logs:          ## Follow logs of all services
	docker compose logs -f

# ── Utilities ──

.PHONY: db-shell help

db-shell:      ## Open psql shell in running PostgreSQL container
	docker compose exec postgres psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

help:          ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*##' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
