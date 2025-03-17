# IIS Sandbox Docker kit

Default port is 8081, can be changed in docker-compose file in line 45

To deploy, execute these commands in the directory:

```
cp .env.example .env
```
```
docker compose up
```

Change passwords, secrets and environment variables in ```.env``` file

Images are already built, and currently hosted on DockerHub.

### Environments variables used :

- FHIR_VERSION: sets the Fhir version of the server, beware that switching FHIR version requires a postgres database
  change or reset
    - R4
  - R5