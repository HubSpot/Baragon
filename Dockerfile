FROM java:8u45-jre
MAINTAINER HubSpot <platform-infrastructure-groups@hubspot.com>

# Used to build hubspot/baragonagentbase image

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y python-setuptools nginx sed && \
    easy_install supervisor && \
    mkdir -p /etc/nginx/conf.d/custom && \
    mkdir -p /etc/nginx/conf.d/proxy && \
    mkdir -p /etc/nginx/conf.d/upstreams

COPY docker/supervisor /etc/supervisor
COPY docker/nginx/conf.d /etc/nginx/conf.d
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf
COPY docker/nginx/start.sh /etc/nginx/start.sh

CMD /usr/local/bin/supervisord -c /etc/supervisor/supervisord.conf