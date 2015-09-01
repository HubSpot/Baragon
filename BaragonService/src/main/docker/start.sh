#!/bin/bash

if [ ${DOCKER_HOST} ]; then
	HOST_AND_PORT=`echo $DOCKER_HOST | awk -F/ '{print $3}'`
	HOST_IP="${HOST_AND_PORT%:*}"
fi

DEFAULT_URI_BASE="http://${HOST_IP:=localhost}:${BARAGON_PORT:=8080}${BARAGON_UI_BASE:=/baragon/v2}"

[[ ! ${BARAGON_ZK_NAMESPACE:-} ]] || args+=( -Ddw.zookeeper.zkNamespace="$BARAGON_ZK_NAMESPACE" )
[[ ! ${BARAGON_ZK_QUORUM:-} ]] || args+=( -Ddw.zookeeper.quorum="$BARAGON_ZK_QUORUM" )
[[ ! ${BARAGON_PORT:-} ]] || args+=( -Ddw.server.connector.port="$BARAGON_PORT" )

args+=( -Ddw.ui.baseUrl="${BARAGON_UI_BASE_URL:=$DEFAULT_URI_BASE}" )

args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Xmx512m )

CONFIG_FILE="${BARAGON_CONFIG_FILE:=/etc/baragon/baragon.yaml}"

exec java "${args[@]}" -jar "/etc/baragon/BaragonService.jar" server $CONFIG_FILE