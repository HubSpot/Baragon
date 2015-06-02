#!/bin/bash
echo "Starting nginx on port ${NGINX_PORT:=80}"
/bin/sed -i "s/NGINX_PORT_PLACEHOLDER/${NGINX_PORT:=80}/" /etc/nginx/conf.d/vhost.conf
nginx