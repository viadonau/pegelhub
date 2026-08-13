#!/bin/sh
set -eu

cat > /usr/share/nginx/html/assets/config.json <<EOF
{
  "apiBaseUrl": "${PH_API_BASE_URL}",
  "keycloak": {
    "url": "${PH_KEYCLOAK_URL}",
    "realm": "${PH_KEYCLOAK_REALM}",
    "clientId": "${PH_KEYCLOAK_CLIENT_ID}"
  }
}
EOF

cat > /etc/nginx/conf.d/default.conf <<EOF
server {
  listen 80;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;
  resolver 127.0.0.11 valid=10s ipv6=off;
  set \$api_upstream ${NGINX_API_UPSTREAM};

  location /api/ {
    proxy_pass \$api_upstream\$request_uri;
    proxy_http_version 1.1;
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
  }

  location / {
    try_files \$uri \$uri/ /index.html;
  }
}
EOF
