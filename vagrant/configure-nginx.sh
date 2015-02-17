#!/bin/bash -x
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Fail fast and fail hard.
set -eo pipefail

mkdir -p /etc/nginx/conf.d/proxy
mkdir -p /etc/nginx/conf.d/upstreams
cat > /etc/nginx/nginx.conf <<EOF
user www-data;
worker_processes 4;
pid /run/nginx.pid;

events {
    worker_connections 768;
    # multi_accept on;
}

http {
    # Basic Settings
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    # server_tokens off;
    # server_names_hash_bucket_size 64;
    # server_name_in_redirect off;

    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Logging Settings
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # Gzip Settings
    gzip on;
    gzip_disable "msie6";

    # Virtual Host Configs
    include /etc/nginx/conf.d/*.conf;
}
EOF
touch /etc/nginx/conf.d/baragon.conf
touch /etc/nginx/conf.d/vhost.conf
cat > /etc/nginx/conf.d/baragon.conf <<EOF
include /etc/nginx/conf.d/upstreams/*.conf;
EOF
cat > /etc/nginx/conf.d/vhost.conf <<EOF
server {
    listen 127.0.0.1:80;
    location /nginx_status {
        stub_status on;
        access_log off;
        allow 127.0.0.1;
        deny all;
    }
}

server {
    listen 80 backlog=4096;
    listen 443 ssl default_server backlog=4096;
    root /var/www/html/;
    location /error/ {
        alias /var/www/error/;
    }
    error_page 404 /error/404.html;
    error_page 500 /error/500.html;
    include conf.d/proxy/*.conf;
}
EOF
service nginx reload

echo "Installed Nginx!"
