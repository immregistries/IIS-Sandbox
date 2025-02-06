# IIS Sandbox Docker kit

Default port is 8081, can be changed in docker-compose file in line 45

To deploy, execute this command in the directory:
```
docker compose up
```

Images are already built, and currently hosted on DockerHub

### Environments variables used :

- FHIR_VERSION: sets the Fhir version of the server, beware that switching FHIR version requires a postgres database
  change or reset
    - R4
    - R5 

