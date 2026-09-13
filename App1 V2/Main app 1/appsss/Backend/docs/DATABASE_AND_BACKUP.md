# Database Architecture, Migrations & Disaster Recovery Guide

## 1. Production Architecture Overview

The Tourist Platform backend uses an enterprise two-tier state store:
- **Primary Relational Store**: PostgreSQL 16+ (ACID transactional guarantees, connection pooling, soft deletes, strict foreign key constraints)
- **High-Speed Cache & In-Memory Store**: Redis 7+ (Token blacklisting, sliding-window rate limiting, OTP with TTL, Celery/Worker job queue)

---

## 2. PostgreSQL Connection Pooling Configuration

Defined in `app/core/database.py` and configurable via `.env`:
```env
DATABASE_URL=postgresql+psycopg://tourist_user:StrongPassword123@db.internal:5432/tourist_db
DB_POOL_SIZE=20
DB_MAX_OVERFLOW=10
DB_POOL_TIMEOUT=30
```

- `pool_size`: Number of permanent database connections kept in pool.
- `max_overflow`: Maximum burst connections during peak traffic (total peak = 30).
- `pool_pre_ping=True`: Verifies connection liveness before checkout to prevent stale disconnect errors.

---

## 3. Alembic Migrations Workflow

### Run Pending Migrations
```bash
alembic upgrade head
```

### Check Current Migration Revision
```bash
alembic current
```

### Create a New Schema Migration
```bash
alembic revision --autogenerate -m "add_partner_payout_tables"
```

### Rollback Last Migration
```bash
alembic downgrade -1
```

---

## 4. Redis Cluster & Eviction Policy

### Recommended `redis.conf` Settings
```conf
maxmemory 2gb
maxmemory-policy volatile-lru
save 900 1
save 300 10
save 60 10000
```
- `volatile-lru`: Evicts keys with an expiration set (e.g. rate limit counters and cached weather/maps) while protecting durable revocation blacklists.

---

## 5. Automated Backup & Recovery Strategy

### A. Daily Automated Logical Backup (Cron)
```bash
#!/bin/bash
# Backup script running daily at 02:00 AM UTC
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/var/backups/postgres"
mkdir -p $BACKUP_DIR

pg_dump -Fc -h localhost -U tourist_user -d tourist_db > "$BACKUP_DIR/tourist_db_$TIMESTAMP.dump"

# Upload to secure cloud bucket (GCS / AWS S3 with KMS encryption)
aws s3 cp "$BACKUP_DIR/tourist_db_$TIMESTAMP.dump" s3://tourist-platform-backups/daily/ --sse aws:kms

# Delete local dumps older than 7 days
find $BACKUP_DIR -type f -name "*.dump" -mtime +7 -delete
```

### B. Point-In-Time Disaster Recovery (PITR)
To restore a backup into a new or recovered database instance:
```bash
# 1. Recreate clean database
dropdb -h localhost -U tourist_user tourist_db
createdb -h localhost -U tourist_user tourist_db

# 2. Restore from custom dump
pg_restore -h localhost -U tourist_user -d tourist_db -j 4 /var/backups/postgres/tourist_db_TARGET.dump

# 3. Verify Alembic migration stamp
alembic current
```
