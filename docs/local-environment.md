# Local environment without Docker

Everything needed to run and test the forum lives under `.proj-env/`, which is
git ignored. This is the workflow used to verify the build on a Windows
machine with no container runtime.

## What goes there

| Component | Version | Notes |
|---|---|---|
| JDK | 11 | `.proj-env/jdk` |
| Maven | 3.8.5 | `.proj-env/apache-maven-3.8.5` |
| MariaDB | 10.6.8 | `.proj-env/mariadb-10.6.8-winx64`, data in `.proj-env/mariadb-data` |
| Redis | 5.0.14 | `.proj-env/redis-server.exe` (tporadowski Windows build) |
| Maven repo | — | `.proj-env/m2work` |
| npm cache | — | `.proj-env/npm-cache` |

MinIO is deliberately absent: when `forum.minio.*` is unset the file service
stores uploads under `forum.storage.local-directory` instead.

## Bringing the services up

MariaDB self bootstraps on first start, so only the password needs setting:

```bash
cd .proj-env
mariadb-10.6.8-winx64/bin/mariadb-install-db.exe --datadir=$PWD/mariadb-data --password=<root-pw> --port=3306 --service=
```

`--service=` (empty) matters: without it the tool tries to register a Windows
service and aborts unless the shell is elevated.

```bash
mariadb-10.6.8-winx64/bin/mariadbd.exe --datadir=$PWD/mariadb-data --basedir=$PWD/mariadb-10.6.8-winx64 \
  --port=3306 --bind-address=127.0.0.1 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --console --skip-name-resolve
```

```bash
mariadb-10.6.8-winx64/bin/mariadb.exe --protocol=TCP -h 127.0.0.1 -P 3306 -u root -p<root-pw> \
  -e "CREATE DATABASE forum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
      CREATE USER 'forum'@'127.0.0.1' IDENTIFIED BY '<pw>';
      GRANT ALL PRIVILEGES ON forum.* TO 'forum'@'127.0.0.1';"
```

```bash
redis-server.exe --port 6379 --bind 127.0.0.1 --save "" --appendonly no
```

## Running the application

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev \
FORUM_DB_USERNAME=forum \
FORUM_DB_PASSWORD=<pw> \
FORUM_REDIS_HOST=127.0.0.1 \
FORUM_REDIS_PORT=6379 \
FORUM_JWT_SECRET=<at least 32 bytes> \
mvn spring-boot:run
```

Flyway applies `V1`–`V3` on the first start and creates every table.

## Tests

```bash
mvn test
```

The mapper integration suite is skipped unless `FORUM_DB_PASSWORD` is set; with
it set, `mvn test` runs those tests against the live schema and rolls back.
