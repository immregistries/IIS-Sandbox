FROM eclipse-temurin:17-jre

# Create the user first so the system registers the name
RUN useradd -m springboot

# Create the data directory and give ownership directly to the named user
RUN mkdir -p /var/data-h2 && chown -R springboot:springboot /var/data-h2

USER springboot
WORKDIR /app

# Copy your built application WAR file
COPY ./iis-web/target/iis.jar /app/iis.jar

EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "iis.jar"]