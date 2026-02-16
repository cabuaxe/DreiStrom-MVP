#!/usr/bin/env bash
# One-time server setup for DreiStrom MVP at 3strom.cabuaxe.com
# Run as root on the target server (217.154.2.230)
set -euo pipefail

APP_DIR=/opt/dreistrom

echo "==> Creating application directory"
mkdir -p "$APP_DIR"/{docker/mysql/init,docker/mysql/conf}

echo "==> Copying nginx site config"
cp "$(dirname "$0")/nginx-3strom.conf" /etc/nginx/sites-available/3strom.conf
ln -sf /etc/nginx/sites-available/3strom.conf /etc/nginx/sites-enabled/3strom.conf

echo "==> Obtaining TLS certificate (if not present)"
if [ ! -d /etc/letsencrypt/live/3strom.cabuaxe.com ]; then
    certbot certonly --nginx -d 3strom.cabuaxe.com --non-interactive --agree-tos -m dev@dreistrom.de
fi

echo "==> Testing nginx config"
nginx -t

echo "==> Reloading nginx"
systemctl reload nginx

echo ""
echo "==> Setup complete. Next steps:"
echo "  1. Create /opt/dreistrom/.env with production secrets (see .env.example)"
echo "  2. Copy docker-compose.prod.yml to /opt/dreistrom/"
echo "  3. Copy docker/mysql/init/* to /opt/dreistrom/docker/mysql/init/"
echo "  4. Configure GitHub secrets (see below)"
echo ""
echo "Required GitHub secrets:"
echo "  DEPLOY_HOST      = 217.154.2.230"
echo "  DEPLOY_USER      = <ssh-user>"
echo "  DEPLOY_SSH_KEY   = <ssh-private-key>"
echo "  GHCR_TOKEN       = <github-pat-with-packages:read>"
echo ""
echo "Required /opt/dreistrom/.env:"
echo "  MYSQL_ROOT_PASSWORD=<secure-password>"
echo "  DB_PASSWORD=<secure-password>"
echo "  FLYWAY_PASSWORD=<secure-password>"
