#!/bin/bash
set -o errexit -o nounset -o pipefail

[[ ! ${BARAGON_AGENT_DOMAIN:-} ]] || args+=( -Ddw.loadBalancerConfig.domain="$BARAGON_AGENT_DOMAIN" )
[[ ! ${BARAGON_AGENT_GROUP:-} ]] || args+=( -Ddw.loadBalancerConfig.name="$BARAGON_AGENT_GROUP" )
[[ ! ${BARAGON_ZK_NAMESPACE:-} ]] || args+=( -Ddw.zookeeper.zkNamespace="$BARAGON_ZK_NAMESPACE" )
[[ ! ${BARAGON_ZK_QUORUM:-} ]] || args+=( -Ddw.zookeeper.quorum="$BARAGON_ZK_QUORUM" )
[[ ! ${BARAGON_PORT:-} ]] || args+=( -Ddw.server.connector.port="$BARAGON_PORT" )

args+=( -Djava.net.preferIPv4Stack=true )
args+=( -Xmx512m )

CONFIG_FILE="${BARAGON_CONFIG_FILE:=/etc/baragon/baragon.yaml}"

exec java "${args[@]}" -jar "/etc/baragon/BaragonAgentService.jar" server $CONFIG_FILE
