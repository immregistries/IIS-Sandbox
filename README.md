# IIS Sandbox with Hapi Fhir Jpa Database
Based on Hapi fhir Jpa starter and IIS Sandbox.

JDK 17 needed.


In production mode HAPI FHIR requires a postgresql database

- On address ``postgresql://localhost:5432/hapi_fhir_iis`` (this can be changed in application.yaml).
 - Generation script: ``/src/main/database/create-postgresql-for-hapi-fhir.sql``


For the current authentication and message log system, a Mysql Database is required, creation script : ``/src/main/database/create-database.sql``.

Complete details for deployment are readable in docker-compose.yml
Run project with docker
```
mvn clean package -Pdocker; docker-compose up;
```

Compile only to run with postgres:
```
mvn clean package
```

Compile and run with embedded H2 database:
```
mvn clean package -Pdev && java -jar --add-opens java.base/java.lang=ALL-UNNAMED target/iis.war
```

## Dependencies not on maven repository

- [Modded HAPIFHIR](https://github.com/cerbeor/hapi-fhir-Subscription-custom/tree/v6.8.3-SUB)
- [vaccination_deduplication](https://github.com/usnistgov/vaccination_deduplication.git)
- [MQE-validator](https://github.com/immregistries/mqe-validator)
- [ForecastConnector](https://github.com/immregistries/VaccineForecastConnector)
- [mismo-match](https://github.com/immregistries/mismo-match)
