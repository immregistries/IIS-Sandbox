# ==========================================
# Stage 1: Build
# ==========================================
FROM eclipse-temurin:17-jdk AS builder

SHELL ["/bin/bash", "-c"]

# Set the working directory
WORKDIR /app

# Install git, curl, jq, maven
RUN apt-get update && apt-get install -y  \
    git  \
    curl  \
    jq  \
    maven \
    && rm -rf /var/lib/apt/lists/*

# Copy the source code into the container
COPY . .

# Ensure the script is executable and run it to create the .jar file
RUN chmod +x dependencies.sh && chmod +x build.sh && ./build.sh

# ==========================================
# Stage 2: Deploy
# ==========================================
FROM tomcat:9.0.117-jdk17-temurin-noble AS tomcat

RUN rm -rf /usr/local/tomcat/webapps/*
RUN rm -rf /usr/local/tomcat/webapps.dist

# Default redirection to Homepage Url
RUN mkdir /usr/local/tomcat/webapps/ROOT
RUN echo '<% response.sendRedirect("/iis/home"); %>' > /usr/local/tomcat/webapps/ROOT/index.jsp

USER root
RUN mkdir ~/data-h2 && chown -R 1001:1001 ~/data-h2
RUN mkdir -p /app/target && chown -R 1001:1001 /app/target
USER 1001
# Used to deactivate dev profile, even if prod profile no longer exists
ENV spring.profiles.active=prod
#ENV spring.jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgres94Dialect
#COPY --from=builder --chown=1001:1001 /app/catalina.properties /usr/local/tomcat/conf/catalina.properties
#COPY --from=builder --chown=1001:1001 /app/server.xml /usr/local/tomcat/conf/server.xml
COPY --from=builder --chown=1001:1001 /app/iis-web/target/*.war /usr/local/tomcat/webapps/iis.war

#RUN apt-get update && apt-get install -y pwgen
#RUN echo "tomcat:$(pwgen -s 16 1)" > /usr/local/tomcat/conf/tomcat-users.txt