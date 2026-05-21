# IIS Sandbox with Hapi Fhir Jpa Database
Based on Hapi fhir Jpa starter and IIS Sandbox.

## Java version

JDK 17

## Dependencies

This project relies on dependencies not hosted on maven repository, the GitHub repositories for the dependencies are
specified in `dependencies.json`

This script allows the quick installation of the dependencies

```bash
mkdir ../temp-dependencies;
./dependencies.sh build ../temp-dependencies
```

To force rebuild and checking out the git revision use ``-f`` flag

```bash
./dependencies.sh build ../temp-dependencies -f
```

## Environment Variables

[example.env](example.env) Provides a working example and skeleton of Environment Variables to set up with H2 databases

Use
```cp example.env .env```
then configure the variables

## Compile and run

Compile and run with embedded H2 database:

```
mvn clean install;
set -a;
source .env;
set +a;
java -jar iis-web/target/iis.war;
```

### Build and run with Docker

Complete details for deployment are readable in docker-compose.yml
Run project with docker
```
mvn clean install; docker-compose up;
```
