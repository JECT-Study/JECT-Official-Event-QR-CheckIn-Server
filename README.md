# JECT Official Event QR Check-In Server

## Local development

Java 26 and Docker Compose are required.

```bash
docker compose up -d mysql
./gradlew bootRun
```

Local MySQL defaults live in `compose.yaml`. Override them with
`LOCAL_MYSQL_PASSWORD` and `LOCAL_MYSQL_ROOT_PASSWORD` when needed.

## Database schema

Hibernate creates the database schema from the JPA entities in both local and
production environments. `spring.jpa.hibernate.ddl-auto` is fixed to `create`,
so every application startup drops the existing tables and data before
recreating the schema.

## Production

- API: `https://checkin-api.ject.kr`
- Health check: `https://checkin-api.ject.kr/actuator/health`
- Runtime: Docker Compose on the JECT Lightsail instance
- Deployment directory: `/opt/ject-checkin`
- Database: MySQL 8.4 Docker container on the same Lightsail instance

Production configuration is supplied through `/opt/ject-checkin/.env`. Never
commit that file. Required variables are documented in `.env.example`.

Build and verify:

```bash
./gradlew clean test bootJar
docker build -t ject-checkin-server:latest .
```

Start the server on Lightsail:

```bash
cd /opt/ject-checkin
sudo docker compose -f compose.prod.yaml up -d
```

Nginx terminates HTTPS and proxies only to `127.0.0.1:8080`. Certbot manages
the Let's Encrypt certificate and automatic renewal. MySQL is not published to
the host network and is reachable only through the private Compose network.

Production deployments run automatically after a successful merge to `main`.
Operational monitoring, alert thresholds, and log commands are documented in
[`docs/operations.md`](docs/operations.md).
