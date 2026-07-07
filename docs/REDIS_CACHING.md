# Redis Caching and Gateway Protection

This project uses Redis in three places:

- **Account Service cache layer** - as L2 distributed cache, together with Caffeine L1 local cache.
- **Account Service Pub/Sub** - to invalidate local Caffeine entries across multiple service instances.
- **API Gateway protection** - for token blacklist and rate limiting.

## 1. Account Service cache design

The cache strategy is **Cache-Aside**:

1. The service first checks **Caffeine** in memory.
2. If the value is not found, it checks **Redis**.
3. If it is still missing, the service reads from PostgreSQL.
4. The result is stored back into Redis and Caffeine.

### Main caches

- `accountsByClient` - accounts list for a client.
- `accountDetails` - account DTOs by id or IBAN.
- `balance` - balance snapshots by IBAN.
- `exchangeRates` - FX rates.

### Key convention

Keys use a readable prefix format:

- `client:{clientId}`
- `iban:{iban}`
- `id:{accountId}`

This makes invalidation easier and keeps the cache organized.

### Expiration policy

Typical starting TTLs:

- Caffeine: short TTL, around a few seconds.
- Redis: 30s to 24h depending on the cache type.

Short-lived data such as balances gets a smaller TTL. Slower-changing data such as exchange rates can stay longer.

## 2. Pub/Sub invalidation

When a write happens in `account-service`, the service:

1. Updates PostgreSQL.
2. Evicts the Redis cache entry.
3. Publishes an invalidation message on Redis Pub/Sub.
4. Other instances receive the message and evict the local Caffeine entry.

This is important because Caffeine is local to each JVM instance.

## 3. Distributed locking with Redisson

Transfers use a Redis distributed lock so that two requests that touch the same account do not execute in parallel.

Example lock key:

- `lock:account:{iban}`

The code acquires the lock before modifying balances and releases it in a `finally` block.

## 4. API Gateway usage

The gateway uses Redis for:

- **Token blacklist** - invalidated access tokens are stored with TTL until expiry.
- **Rate limiting** - request counters are kept in Redis so the limit works across multiple gateway instances.

This is better than in-memory storage because it works correctly in a multi-instance deployment.

## 5. Local development

Redis runs in the shared Docker Compose stack.

Useful commands:

```bash
docker compose -p online-internet-banking-dev -f docker-compose.yml -f docker-compose.override.yml up -d redis
redis-cli ping
```

Expected result:

- `PONG`

## 6. For thesis presentation

A short explanation:

- PostgreSQL is the source of truth.
- Redis improves speed and protects the system against repeated expensive reads.
- Caffeine gives very fast local reads.
- Redis synchronizes the instances and supports gateway security features.
