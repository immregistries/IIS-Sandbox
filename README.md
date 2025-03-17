# IIS Sandbox Docker kit

Kit to easily deploy the IIS sandbox from the images hosted on dockerhub.

Default port is 8081, can be changed in docker-compose file in line 45

``.env`` file should be copied from ``.env.example`` and modified to change default passwords and secrets.

To quickly deploy with default configuration

```
cp .env.example .env;
docker compose up;
```

or use bash script

```
bash deploy.sh
```

If ``.env`` already defined, run this command to deploy in docker a container

```
docker compose up;
```

Change passwords, secrets and environment variables in ```.env``` file

### Environments variables used :
- FHIR_VERSION: sets the Fhir version of the server, beware that switching FHIR version requires a postgres database
  change or reset
  - R4
  - R5
- *DATABASE_CREDENTIALS*: see ``.env.example``
