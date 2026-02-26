# ── Frontend Dockerfile — Multi-Stage Build ──
# Stage 1 (build): Install deps + build with Vite → produces static files in dist/.
# Stage 2 (runner): Serve static files with nginx.
# Same pattern as the backend Dockerfile, but for the React SPA.

# Build stage: compile the React (Vite) app
FROM node:24-alpine AS build

WORKDIR /app

# Copy package files first for Docker layer caching
# (dependencies only re-install when these files change).
COPY package.json pnpm-lock.yaml ./
RUN corepack enable \
  && corepack prepare pnpm@latest --activate \
  && pnpm install --frozen-lockfile

# Copy source and build
COPY . .
RUN pnpm build

# Runtime stage: serve static files with nginx
# ≈ Serving the dist/ folder from a Vite build.
FROM nginx:1.27-alpine AS runner

# Copy the built static files into nginx's default serve directory.
COPY --from=build /app/dist /usr/share/nginx/html
# Custom nginx config for SPA routing (all routes → index.html).
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
