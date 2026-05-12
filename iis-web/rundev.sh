set -a;
source ../dev.env;
set +a;
mvn clean spring-boot:run -Pdev,jetty -DaddResources=True -Dspring-boot.run.arguments="--server.servlet.context-path=/iis"