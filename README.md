# Computer Checkout

A web application where students reserve computers for a time slot. It combines a relational
database, a Redis cache, HDFS file storage and a local LLM chat.

| Part | Technology | Role |
|---|---|---|
| Web app | Spring Boot 4.1, Java 21, Thymeleaf | Pages and REST API |
| Database | MySQL 8.4 | Source of truth (students, computers, reservations) |
| Cache | Redis 7 | Fast reads, write-behind for reservation updates |
| File storage | Hadoop HDFS (WebHDFS) | Student photos |
| Chat | Ollama + llama3.2 | Chat page |
| Reverse proxy | Nginx | Entry point in front of the Java app |

## Architecture

```
Browser --> Nginx (:80) --> Spring Boot (:8080) --+--> Redis   (:6379)
                                                  +--> MySQL   (:3307)
                                                  +--> HDFS    (:9870 WebHDFS)
                                                  +--> Ollama  (:11434)
Browser --(student photos)--> HDFS (:9870)
```

## Requirements

- Docker Desktop (Compose v2)
- Java 21 and Maven (or use the Maven wrapper `mvnw` in `app/`)
- About 5 GB of free disk space (Hadoop and Ollama images, llama3.2 model)
- Optional: NVIDIA GPU for faster chat (see Notes)

## Setup

1. **Create the `.env` file** in the project root (next to `docker-compose.yml`):
   ```bash
   cp .env.example .env
   ```
   On Windows PowerShell use `copy .env.example .env`. Edit `.env` and set `MYSQL_ROOT_PASSWORD`.

2. **Start the services** (MySQL, Redis, HDFS, Ollama, Nginx):
   ```bash
   docker compose up -d
   ```
   The first start creates the tables and loads the sample data from `db/init/`.

3. **Download the LLM model** (only once, about 2 GB):
   ```bash
   docker exec pro2-ollama ollama pull llama3.2
   ```

4. **Run the application**:
   ```bash
   cd app
   mvn spring-boot:run
   ```

5. **Open the app**: http://localhost (through Nginx) or http://localhost:8080 (direct).

## Pages

| URL | Page |
|---|---|
| `/` | Reservations (JOIN of reservations, students and computers). Add, edit, delete |
| `/students` | Students: add, edit, delete, upload photo (stored in HDFS) |
| `/computers` | Computers: add, edit, delete |
| `/chat` | Chat with the local llama3.2 model |

The reservation form only lets you pick a computer that is free in the selected time range
(busy computers are red and disabled).

## REST API

| Method | URL | Description |
|---|---|---|
| GET | `/api/reservations` | List reservations (JOIN view) |
| POST | `/api/reservations` | Create (`studentNumber`, `computerId`, `reservationDate`, `returnDate`) |
| PUT | `/api/reservations/{id}` | Update dates |
| DELETE | `/api/reservations/{id}` | Delete |
| GET/POST | `/api/students`, `/api/computers` | List / create |
| PUT/DELETE | `/api/students/{id}`, `/api/computers/{id}` | Update / delete |
| POST | `/api/students/{id}/photo` | Upload a photo (multipart, field `file`, jpg or png, max 2 MB) |
| POST | `/api/chat` | Chat (`{"message": "..."}`) |

Status codes: 200/201/204 success, 400 invalid input, 404 not found, 409 conflict
(overlapping reservation, duplicate number, record still in use), 413 file too large.

## How Redis is used

| Operation | Order | Why |
|---|---|---|
| **Read** | Redis first, MySQL (JOIN) on a miss, then the result is stored in Redis (TTL 5 min) | Faster responses (cache-aside) |
| **Create / Delete reservation** | MySQL first, then Redis | MySQL generates the id and runs the checks (overlap, availability) |
| **Update reservation** | Redis first, MySQL later (`@Scheduled` every 30 s) | Write-behind: fast response, batched write. Pending changes are kept in `reservation:pending` |
| **Students / computers** | MySQL first, then the Redis list is updated (reservation cache is cleared) | The reservation view contains student and computer fields |
| **Photo upload** | HDFS, then MySQL (file name), then Redis | The file must exist before its name is saved |

Redis keys: `reservation:view`, `reservation:pending`, `student:view`, `computer:view`
(each list is one Redis hash: field = id, value = JSON).

Note: a pending update lives only in Redis until the next flush (at most 30 seconds).

## Useful commands

```bash
docker exec pro2-redis redis-cli keys "*"                    # list Redis keys
docker exec pro2-redis redis-cli ttl reservation:view        # remaining TTL (seconds)
docker exec pro2-redis redis-cli hgetall reservation:pending # updates waiting for MySQL
docker exec pro2-redis redis-cli del reservation:view        # force a cache miss
docker exec pro2-namenode hdfs dfs -ls /user/hadoop/images   # photos in HDFS
```

On Git Bash (Windows) put `MSYS_NO_PATHCONV=1` before the `hdfs` command, otherwise the path is rewritten.
Other addresses: HDFS web UI http://localhost:9870 (browse `/user/hadoop/images`), MySQL on `localhost:3307`.

## Project structure

```
app/            Spring Boot application (controller, service, repository, model, dto, templates)
db/init/        MySQL schema and sample data (runs only when the MySQL volume is empty)
nginx/          Nginx reverse proxy configuration
docker-compose.yml
.env.example
```

## Notes

- **No NVIDIA GPU?** In `docker-compose.yml`, remove the `deploy:` block of the `ollama` service. The chat still works on the CPU, only slower.
- **Port 80 already in use?** Change `"80:80"` to `"8000:80"` in the `nginx` service and open http://localhost:8000.
- **Reset the data:** `docker compose down -v` deletes all volumes (MySQL, HDFS, Ollama model). Do not run it if you want to keep your data. Run `docker compose down` to stop without deleting anything.
- Sample data is loaded only on the first start. Student photos are not part of the sample data; upload them from the Students page.
- The Nginx container reaches the app on the host through `host.docker.internal:8080`, so start the application on the host as described above.
