#!/bin/bash
/usr/bin/sed -i "s/##NGINX_PORT##/${NGINX_PORT:=80}/" /etc/nginx/conf.d/vhost.conf
nginx