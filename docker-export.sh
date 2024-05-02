#! /bin/bash
mvn clean install -Pprod;
docker build . -t clemhen/iis-sandbox:local --platform=linux/amd64;
docker save clemhen/iis-sandbox:local -o target/iis-sandbox-image.tar