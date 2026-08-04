#!/bin/bash
# Smart Queue Management System — Local Development Helper
# Usage: ./scripts/dev.sh [command]

set -e

COMPOSE_FILE="docker-compose.yml"
ENV_FILE=".env"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info()    { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

check_env() {
  if [ ! -f "$ENV_FILE" ]; then
    log_error ".env file not found. Copy from .env.example:"
    echo "  cp .env.example .env"
    echo "  Then fill in your secrets."
    exit 1
  fi
  log_success ".env file found"
}

check_docker() {
  if ! docker info > /dev/null 2>&1; then
    log_error "Docker is not running. Please start Docker Desktop."
    exit 1
  fi
  log_success "Docker is running"
}

cmd_infra() {
  log_info "Starting infrastructure only (Postgres, Redis, Kafka)..."
  check_env
  check_docker
  docker compose -f "$COMPOSE_FILE" up -d postgres redis zookeeper kafka
  log_info "Waiting for health checks..."
  sleep 10
  docker compose ps
  log_success "Infrastructure is up. Services:"
  echo "  PostgreSQL → localhost:5432"
  echo "  Redis      → localhost:6379"
  echo "  Kafka      → localhost:9092"
}

cmd_up() {
  log_info "Starting full stack..."
  check_env
  check_docker
  docker compose -f "$COMPOSE_FILE" up -d
  log_success "All services starting. Dashboards:"
  echo "  API Gateway  → http://localhost:8080"
  echo "  Auth Service → http://localhost:8081/swagger-ui.html"
  echo "  Queue Service→ http://localhost:8082/swagger-ui.html"
  echo "  Grafana      → http://localhost:3000  (admin/admin)"
  echo "  Prometheus   → http://localhost:9090"
}

cmd_down() {
  log_info "Stopping all services..."
  docker compose -f "$COMPOSE_FILE" down
  log_success "All services stopped"
}

cmd_reset() {
  log_warn "This will DELETE all data (volumes). Are you sure? [y/N]"
  read -r confirm
  if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
    docker compose -f "$COMPOSE_FILE" down -v
    log_success "All containers and volumes removed"
  else
    log_info "Reset cancelled"
  fi
}

cmd_logs() {
  SERVICE=${2:-""}
  if [ -z "$SERVICE" ]; then
    docker compose -f "$COMPOSE_FILE" logs -f --tail=100
  else
    docker compose -f "$COMPOSE_FILE" logs -f --tail=100 "$SERVICE"
  fi
}

cmd_status() {
  docker compose -f "$COMPOSE_FILE" ps
}

cmd_test() {
  log_info "Running unit tests (no containers needed)..."
  mvn test -pl auth-service,queue-service \
    -Dtest="**/*Test" \
    -DexcludeTests="**/*IntegrationTest" \
    --no-transfer-progress
  log_success "Unit tests complete"
}

cmd_test_integration() {
  log_info "Running integration tests (Testcontainers — Docker required)..."
  check_docker
  mvn test -pl auth-service,queue-service \
    -Dtest="**/*IntegrationTest" \
    --no-transfer-progress
  log_success "Integration tests complete"
}

cmd_build() {
  log_info "Building all modules..."
  mvn clean package -DskipTests --no-transfer-progress
  log_success "Build complete"
}

cmd_health() {
  log_info "Checking service health..."
  services=(
    "Gateway:8080"
    "Auth:8081"
    "Queue:8082"
    "Notification:8083"
    "Analytics:8084"
    "Admin:8085"
  )
  for entry in "${services[@]}"; do
    name="${entry%%:*}"
    port="${entry##*:}"
    if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
      log_success "$name Service (:$port) — UP"
    else
      log_error "$name Service (:$port) — DOWN or unreachable"
    fi
  done
}

cmd_help() {
  echo ""
  echo "Smart Queue Management System — Dev Helper"
  echo ""
  echo "Usage: ./scripts/dev.sh [command]"
  echo ""
  echo "Commands:"
  echo "  infra           Start only PostgreSQL, Redis, Kafka"
  echo "  up              Start full stack (all services)"
  echo "  down            Stop all services"
  echo "  reset           Stop all services and delete all data"
  echo "  logs [service]  Tail logs (all services or specific one)"
  echo "  status          Show running containers"
  echo "  health          Check all service health endpoints"
  echo "  test            Run unit tests"
  echo "  test-it         Run integration tests (needs Docker)"
  echo "  build           Build all Maven modules"
  echo "  help            Show this help"
  echo ""
  echo "Examples:"
  echo "  ./scripts/dev.sh infra"
  echo "  ./scripts/dev.sh logs queue-service"
  echo "  ./scripts/dev.sh health"
}

COMMAND=${1:-"help"}
case "$COMMAND" in
  infra)          cmd_infra ;;
  up)             cmd_up ;;
  down)           cmd_down ;;
  reset)          cmd_reset ;;
  logs)           cmd_logs "$@" ;;
  status)         cmd_status ;;
  health)         cmd_health ;;
  test)           cmd_test ;;
  test-it)        cmd_test_integration ;;
  build)          cmd_build ;;
  help|--help|-h) cmd_help ;;
  *)
    log_error "Unknown command: $COMMAND"
    cmd_help
    exit 1
    ;;
esac
