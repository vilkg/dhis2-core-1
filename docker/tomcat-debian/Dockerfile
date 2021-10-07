ARG BASE_IMAGE
ARG TOMCAT_IMAGE

FROM $BASE_IMAGE as serve

FROM $TOMCAT_IMAGE

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

ENV WAIT_FOR_DB_CONTAINER=""

ENV DHIS2_HOME=/DHIS2_home

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    mkdir $DHIS2_HOME && \
    adduser --system --disabled-password --group tomcat && \
    echo 'tomcat' >> /etc/cron.deny && \
    echo 'tomcat' >> /etc/at.deny

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        util-linux \
        bash \
        unzip \
        fontconfig

COPY ./shared/wait-for-it.sh /usr/local/bin/
COPY ./tomcat-debian/docker-entrypoint.sh /usr/local/bin/

RUN chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    chmod +rx /usr/local/bin/wait-for-it.sh

COPY ./shared/server.xml /usr/local/tomcat/conf

COPY --from=serve /srv/dhis2/dhis.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=serve /srv/dhis2/sha256sum.txt /usr/local/tomcat/webapps/sha256sum.txt
COPY --from=serve /srv/dhis2/md5sum.txt /usr/local/tomcat/webapps/md5sum.txt

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]
