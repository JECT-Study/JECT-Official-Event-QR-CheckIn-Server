# Production operations

## Deployment

Pull requests targeting `main` must pass the `CI/CD / test-build` check. A
successful merge to `main` builds the application and deploys it to the
Lightsail production server.

The deployment replaces only the Spring Boot application container. MySQL,
n8n, PostgreSQL, Nginx, and the JECT bot stay running. The previous application
image is retained as `ject-checkin-server:rollback` and is restored
automatically when the new container does not become healthy within 90 seconds.

## Monitoring

`ject-checkin-monitor.timer` runs every minute and checks:

- available memory below 150 MiB;
- swap usage at or above 80%;
- root disk usage at or above 85%;
- Spring Boot, MySQL, n8n, and PostgreSQL containers;
- Nginx and the PM2-managed JECT bot;
- the public API and n8n HTTPS health endpoints.

An alert is sent to Discord after three consecutive failures. Recovery is sent
after two consecutive successful checks. An unresolved alert is repeated once
per hour.

Inspect the latest safe status summary:

```bash
sudo cat /var/lib/ject-checkin-monitor/status
```

Inspect monitor logs:

```bash
sudo journalctl -u ject-checkin-monitor.service --since "1 hour ago"
```

## Logs

Application and database container logs:

```bash
cd /opt/ject-checkin
sudo docker compose -f compose.prod.yaml logs --since 30m app mysql
```

n8n and PostgreSQL logs:

```bash
cd /opt/n8n
sudo docker compose logs --since 30m n8n postgres
```

JECT bot logs:

```bash
pm2 logs ject-bot --lines 100 --nostream
```

Nginx logs:

```bash
sudo tail -n 100 /var/log/nginx/access.log
sudo tail -n 100 /var/log/nginx/error.log
```

Docker logs are capped at three 10 MiB files per container. PM2 logs are
rotated daily, capped at 10 MiB per file, and retained for five rotations.

Database backups are intentionally not scheduled.
