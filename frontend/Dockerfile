FROM node:24-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine

ENV PH_API_BASE_URL=/api/v1 \
    PH_KEYCLOAK_URL=http://pegelhub-keycloak.test:8082 \
    PH_KEYCLOAK_REALM=pegelhub \
    PH_KEYCLOAK_CLIENT_ID=pegelhub-frontend \
    NGINX_API_UPSTREAM=http://core-app:8080

COPY --from=build /app/dist/pegelhub-frontend-next/browser /usr/share/nginx/html
COPY docker/entrypoint.sh /docker-entrypoint.d/40-pegelhub-runtime.sh
RUN chmod +x /docker-entrypoint.d/40-pegelhub-runtime.sh

EXPOSE 80

HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=12 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
