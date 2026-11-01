# Fleet Telemetry Platform — Cloud & Live Deployment Guide

This guide covers deploying the **Fleet Telemetry Platform** live to cloud hosting platforms.

---

## Architecture Overview

A live deployment of the platform requires 3 services:
1. **Ingest & Consumer Application** (Spring Boot 3, Java 17, Docker)
2. **PostgreSQL 15** (Schema in `db/init/01_schema.sql`, Seed data in `02_seed.sql`)
3. **Kafka Cluster** (Topics: `telemetry.raw` with 12 partitions, `telemetry.dlq` with 3 partitions, `alerts.out` with 3 partitions)

---

## Option 1: Railway (Recommended — Fastest & Easiest)

Railway supports multi-service architectures and automatically deploys Docker containers from GitHub.

### Step 1: Connect GitHub Repository
1. Log into [Railway](https://railway.app/).
2. Click **New Project** → **Deploy from GitHub repo**.
3. Select `Anubhav104401/MotorQ-practise-project`.
4. Railway will automatically detect the [Dockerfile](file:///c:/Users/anubh/Practise%20session/MotorQ/Dockerfile).

### Step 2: Add Postgres
1. In your project canvas, click **New** → **Database** → **PostgreSQL**.
2. Once deployed, click the Postgres service → **Variables** or **Connect**.
3. Connect with any SQL client (or `psql`) and run:
   - `db/init/01_schema.sql` (Creates tables)
   - `db/init/02_seed.sql` (Loads test seed data)

### Step 3: Add Kafka
1. In the project canvas, click **New** → **Template** → Search for **Kafka** (or deploy an [Upstash Kafka](https://upstash.com/) / [Aiven Kafka](https://aiven.io/) cluster).
2. Create the required topics:
   - `telemetry.raw` (12 partitions)
   - `telemetry.dlq` (3 partitions)
   - `alerts.out` (3 partitions)

### Step 4: Configure App Environment Variables
In your web service on Railway, add these environment variables:
| Variable | Value | Description |
|---|---|---|
| `PORT` | `8080` | Web server listening port |
| `DB_HOST` | `${{Postgres.PGHOST}}` | Database host |
| `DB_PORT` | `${{Postgres.PGPORT}}` | Database port |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` | Database name |
| `DB_USER` | `${{Postgres.PGUSER}}` | Database user |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` | Database password |
| `KAFKA_BOOTSTRAP` | `<your-kafka-broker-host:port>` | Kafka bootstrap server |
| `CONSUMER_ENABLED` | `true` | Runs both Ingest API and consumer |

4. Railway will build and deploy the container. Verify at `https://<your-app-domain>/health`.

---

## Option 2: Render (Free Tier Friendly)

### Step 1: Push Blueprint or Deploy Web Service
1. Log into [Render](https://render.com/).
2. Click **Blueprints** → **New Blueprint Instance**.
3. Select your repository `Anubhav104401/MotorQ-practise-project`.
4. Render will read [render.yaml](file:///c:/Users/anubh/Practise%20session/MotorQ/render.yaml) and automatically provision:
   - Web Service (`fleet-telemetry`)
   - PostgreSQL Database (`fleet-postgres`)

### Step 2: Kafka Setup with Upstash
1. Go to [Upstash Console](https://console.upstash.com/kafka) (free tier available).
2. Create a Kafka cluster and create the topics:
   - `telemetry.raw`
   - `telemetry.dlq`
   - `alerts.out`
3. Copy the **Bootstrap Server** endpoint and SASL credentials.
4. Set `KAFKA_BOOTSTRAP` in Render Web Service environment variables.

---

## Option 3: Single Cloud VM (AWS EC2 / DigitalOcean Droplet / Hetzner)

If you have a cloud Linux server (Ubuntu 22.04 / 24.04):

### Step 1: Install Docker
```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
```

### Step 2: Clone & Deploy Entire Stack
```bash
git clone https://github.com/Anubhav104401/MotorQ-practise-project.git
cd MotorQ-practise-project

# Launch all 4 services (Postgres, Kafka, Kafka UI, and App):
docker compose -f docker-compose.prod.yml up -d --build
```

### Step 3: Verify Everything
```bash
# Check status
docker compose -f docker-compose.prod.yml ps

# Check app health endpoint
curl http://localhost:8080/health
# Expected: {"status":"UP","db":"up","kafka_bootstrap":"kafka:29092"}

# Access Kafka UI in browser:
http://<your-server-ip>:8081
```

---

## Testing the Live Deployment

From your local machine or server, run the simulator against the live endpoint:
```bash
# Test Ingest API validation (malformed payloads)
python3 simulator/simulate.py --api https://<your-deployed-host> --mode malformed

# Send real vehicle stream (5 vehicles, 10 pings each)
python3 simulator/simulate.py --api https://<your-deployed-host> --vehicles 5 --count 10

# Test idempotency (duplicate mode)
python3 simulator/simulate.py --api https://<your-deployed-host> --mode duplicate --vehicles 5 --count 10
```
