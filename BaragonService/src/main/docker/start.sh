#!/bin/bash

if [ ${DOCKER_HOST} ]; then
    HOST_AND_PORT=`echo $DOCKER_HOST | awk -F/ '{print $3}'`
    DEFAULT_HOSTNAME="${HOST_AND_PORT%:*}"
    BARAGON_HOSTNAME="${BARAGON_HOSTNAME:=$DEFAULT_HOSTNAME}"
fi

DEFAULT_URI_BASE="http://${BARAGON_HOSTNAME:=localhost}:${BARAGON_PORT:=8080}${BARAGON_UI_BASE:=/baragon/v2}"

[[ ! ${BARAGON_ZK_NAMESPACE:-} ]] || args+=( -Ddw.zookeeper.zkNamespace="$BARAGON_ZK_NAMESPACE" )
[[ ! ${BARAGON_ZK_QUORUM:-} ]] || args+=( -Ddw.zookeeper.quorum="$BARAGON_ZK_QUORUM" )
[[ ! ${BARAGON_PORT:-} ]] || args+=( -Ddw.server.connector.port="$BARAGON_PORT" )
[[ ! ${BARAGON_AUTH_ENABLED:-} ]] || args+=( -Ddw.auth.enabled="$BARAGON_AUTH_ENABLED" )
[[ ! ${BARAGON_AUTH_KEY:-} ]] || args+=( -Ddw.auth.key="$BARAGON_AUTH_KEY" )
[[ ! ${BARAGON_HOSTNAME:-} ]] || args+=( -Ddw.hostname="$BARAGON_HOSTNAME" )

args+=( -Ddw.ui.baseUrl="${BARAGON_UI_BASE_URL:=$DEFAULT_URI_BASE}" )

args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Xmx512m )

CONFIG_FILE="${BARAGON_CONFIG_FILE:=/etc/baragon/baragon.yaml}"

exec java "${args[@]}" -jar "/etc/baragon/BaragonService.jar" server $CONFIG_FILE