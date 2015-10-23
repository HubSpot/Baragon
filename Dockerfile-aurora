FROM java:8u45-jre
MAINTAINER HubSpot <paas@hubspot.com>

# Used to build hubspot/baragonagentbase-aurora image

RUN apt-get update && \
    apt-get install -y python-setuptools sed \
      libapr1-dev libsasl2-dev libcurl4-nss-dev libsvn-dev && \
    easy_install supervisor && \
    apt-get install -y -t jessie-backports nginx=1.9.4-1~bpo8+1 && \
    mkdir -p /etc/nginx/conf.d/custom && \
    mkdir -p /etc/nginx/conf.d/proxy && \
    mkdir -p /etc/nginx/conf.d/upstreams && \
    apt-get clean && \
      rm -rf /var/cache/apt/* && \
      rm -rf /var/lib/apt/lists/* && \
      rm -rf /tmp/* && \
      rm -rf /var/tmp/*

COPY docker/supervisor /etc/supervisor
COPY docker/nginx/conf.d /etc/nginx/conf.d
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf
COPY docker/nginx/start.sh /etc/nginx/start.sh

CMD /usr/local/bin/supervisord -c /etc/supervisor/supervisord.conf
