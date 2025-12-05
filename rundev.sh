set -a;
source .env.dev;
set +a;
mvn clean spring-boot:run -Pdev-r4,jetty -DaddResources=True -Dspring-boot.run.arguments="--server.port=8080 --server.servlet.context-path=/iis"