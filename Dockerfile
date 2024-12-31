########### bitnami tomcat version is suitable for debugging and comes with a shell
########### it can be built using eg. `docker build --target tomcat .`
FROM bitnami/tomcat:9.0 AS tomcat

RUN rm -rf /opt/bitnami/tomcat/webapps/ROOT && \
    rm -rf /opt/bitnami/tomcat/webapps_default/ROOT


RUN mkdir /opt/bitnami/tomcat/webapps_default/ROOT
RUN echo '<% response.sendRedirect("/iis/home"); %>' > /opt/bitnami/tomcat/webapps_default/ROOT/index.jsp

USER root
RUN mkdir -p /target && chown -R 1001:1001 target
USER 1001

COPY --chown=1001:1001 catalina.properties /opt/bitnami/tomcat/conf/catalina.properties
COPY --chown=1001:1001 server.xml /opt/bitnami/tomcat/conf/server.xml
COPY --chown=1001:1001 target/iis.war /opt/bitnami/tomcat/webapps_default/iis.war

ENV TOMCAT_PASSWORD="28y341834uf8u3bfppkaebiThisIsSomehtingThatShouldBeModified917628oeruoipi3u267yui9877tu398nmjq09o321"

