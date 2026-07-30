# JECT Official Event QR Check-In Server

## Local development

Java 26 and Docker Compose are required.

```bash
docker compose up -d mysql
./gradlew bootRun
```

Local MySQL defaults live in `compose.yaml`. Override them with
`LOCAL_MYSQL_PASSWORD` and `LOCAL_MYSQL_ROOT_PASSWORD` when needed.

## Production

- API: `https://checkin-api.ject.kr`
- Health check: `https://checkin-api.ject.kr/actuator/health`
- Runtime: Docker Compose on the JECT Lightsail instance
- Deployment directory: `/opt/ject-checkin`
- Database: private RDS MySQL database over VPC peering and verified TLS

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
the Let's Encrypt certificate and automatic renewal.
