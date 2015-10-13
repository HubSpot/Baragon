FROM java:8u45-jre
MAINTAINER HubSpot <paas@hubspot.com>

# Used to build hubspot/baragonagentbase image

RUN apt-get update && \
    apt-get install -y python-setuptools nginx sed \
      libapr1-dev libsasl2-dev libcurl4-nss-dev libsvn-dev && \
    easy_install supervisor && \
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
