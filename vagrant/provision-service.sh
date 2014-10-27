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

function install_baragon_config {
  mkdir -p /etc/baragon
  cat > /etc/baragon/baragon_service.yaml <<EOF
# singularity-related config:
server:
  type: simple
  applicationContextPath: /baragon/v1
  connector:
    type: http
    port: 8080
  requestLog:
    appenders:
      - type: file
        currentLogFilename: ../logs/access.log
        archivedLogFilenamePattern: ../logs/access-%d.log.gz

zookeeper:
  quorum: localhost:2181
  zkNamespace: baragon
  sessionTimeoutMillis: 60000
  connectTimeoutMillis: 5000
  retryBaseSleepTimeMilliseconds: 1000
  retryMaxTries: 3

EOF
}

function build_baragon {
  cd /baragon
  sudo -u vagrant HOME=/home/vagrant mvn clean package
}

function install_baragon {
  mkdir -p /var/log/baragon
  mkdir -p /usr/local/baragon/bin
  cp /baragon/BaragonService/target/BaragonService-*-SNAPSHOT.jar /usr/local/baragon/bin/baragon_service.jar

  cat > /etc/init/baragon_service.conf <<EOF
#!upstart
description "Baragon Service"

env PATH=/usr/local/baragon/bin:/usr/local/sbin:/usr/sbin:/sbin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin

start on stopped rc RUNLEVEL=[2345]

respawn

exec java -Xmx512m -Djava.net.preferIPv4Stack=true -jar /usr/local/baragon/bin/baragon_service.jar server /etc/baragon/baragon_service.yaml >> /var/log/baragon/baragon_service.log 2>&1
EOF
}


function stop_baragon {
  set +e  # okay if this fails (i.e. not installed)
  service baragon_service stop
  set -e
}

function start_baragon {
  service baragon_service start
}

stop_baragon
install_baragon_config
build_baragon
install_baragon
start_baragon

echo "Great Job!"
