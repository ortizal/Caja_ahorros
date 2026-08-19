#!/usr/bin/env bash
#
# deploy.sh — Despliegue de la Caja de Ahorros ALANTEK en el servidor
# (192.168.1.43, Linux).
#
#   Frontend : build Angular con base-href /caja-ahorros/ y copia a
#              /var/www/caja-ahorros  (nginx NATIVO; el site se configura
#              manualmente en sites-available, ver NGINX_CAJA_AHORROS.txt).
#   Backend  : imagen Docker, publicada en el puerto 8083 -> 8080 del contenedor.
#
# Uso:
#   ./deploy.sh                     # despliega todo (frontend + backend)
#   ./deploy.sh backend             # solo backend (Docker, puerto 8083)
#   ./deploy.sh frontend            # solo frontend (build + /var/www)
#
set -euo pipefail

APP_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$APP_ROOT/frontend"
BACKEND_DIR="$APP_ROOT/backend"

WEB_ROOT="${WEB_ROOT:-/var/www/caja-ahorros}"
BASE_HREF="${BASE_HREF:-/caja-ahorros/}"
FRONTEND_DIST="dist/caja-ahorros-frontend"

IMAGE_NAME="${IMAGE_NAME:-caja-ahorros-backend}"
CONTAINER_NAME="${CONTAINER_NAME:-caja-ahorros-backend}"
HOST_PORT="${BACKEND_PORT:-8083}"
CONTAINER_PORT=8080

# Entorno de la base de datos (PostgreSQL en el servidor 192.168.1.43).
DB_URL="${DB_URL:-jdbc:postgresql://host.docker.internal:5432/caja_ahorros}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-086411421}"
JWT_SECRET="${JWT_SECRET:-caja-ahorros-alantek-secreto-jwt-2026-debe-tener-32-bytes-minimo}"
CORS_ORIGINS="${CORS_ORIGINS:-https://alan-tek.com,http://192.168.1.43}"

SUDO=""
if [ "$(id -u)" -ne 0 ]; then SUDO="sudo"; fi

step() { echo; echo "===> $1"; }

deploy_frontend() {
  step "Frontend: build Angular (base-href=$BASE_HREF)"
  cd "$FRONTEND_DIR"
  npm ci --silent
  npx ng build --base-href="$BASE_HREF"

  step "Frontend: copiando $FRONTEND_DIST a $WEB_ROOT"
  DIST_DIR="$FRONTEND_DIR/$FRONTEND_DIST"
  if [ -d "$DIST_DIR/browser" ]; then DIST_DIR="$DIST_DIR/browser"; fi
  $SUDO mkdir -p "$WEB_ROOT"
  $SUDO rm -rf "$WEB_ROOT"/*
  $SUDO cp -r "$DIST_DIR/"* "$WEB_ROOT/"
  $SUDO chown -R www-data:www-data "$WEB_ROOT"
  $SUDO chmod -R 755 "$WEB_ROOT"
  echo "Frontend desplegado en $WEB_ROOT (raiz http://192.168.1.43/caja-ahorros/)"
}

deploy_backend() {
  step "Backend: compilando JAR (skip tests)"
  cd "$BACKEND_DIR"
  chmod +x ./mvnw
  ./mvnw -q -DskipTests package

  step "Backend: construyendo imagen Docker ($IMAGE_NAME)"
  docker build -t "$IMAGE_NAME" .

  step "Backend: publicando contenedor en puerto $HOST_PORT"
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  docker run -d \
    --name "$CONTAINER_NAME" \
    --restart unless-stopped \
    --add-host=host.docker.internal:host-gateway \
    -p "$HOST_PORT:$CONTAINER_PORT" \
    -e DB_URL="$DB_URL" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -e JWT_SECRET="$JWT_SECRET" \
    -e CORS_ALLOWED_ORIGINS="$CORS_ORIGINS" \
    "$IMAGE_NAME"

  echo "Backend corriendo: http://192.168.1.43:$HOST_PORT (API /api/v1 proxied por nginx)"
}

case "${1:-todo}" in
  backend)  deploy_backend ;;
  frontend) deploy_frontend ;;
  todo|"")  deploy_frontend; deploy_backend ;;
  *) echo "Uso: $0 [todo|backend|frontend]" >&2; exit 1 ;;
esac

step "Despliegue completado."
echo "  App:  http://192.168.1.43/caja-ahorros/"
echo "  API:  http://192.168.1.43:8083/api/v1"
echo "  Logs backend: docker logs -f $CONTAINER_NAME"
