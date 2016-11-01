-- name: create-metrics-table!
CREATE TABLE IF NOT EXISTS
metrics (
  id SERIAL PRIMARY KEY,
  name VARCHAR(32) unique NOT NULL
);

-- name: create-metrics-history-table!
-- if a value already exists for the given metric_id + timestamp combination
-- the newer value is updated
CREATE TABLE IF NOT EXISTS
metrics_history(
  metric_id INT REFERENCES metrics NOT NULL,
  value DOUBLE PRECISION NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  unique(metric_id, timestamp)
);

-- name: create-metric<!
INSERT INTO metrics (name)
VALUES (:name);

-- name: create-metric-history<!
INSERT INTO metrics_history (metric_id, value, timestamp)
VALUES (:metric_id, :value, :timestamp)
ON CONFLICT (metric_id, timestamp)
DO UPDATE
SET value = :value;

-- name: query-by-name
SELECT *
FROM metrics
WHERE name = :name;

-- name: query-all-metrics
SELECT name, value, timestamp
FROM metrics
JOIN metrics_history
ON id = metric_id
ORDER BY timestamp DESC
LIMIT :limit
OFFSET :offset;

-- name: query-current-metric-count
SELECT count(*)
FROM metrics_history;

-- name: query-by-timestamp
SELECT *
FROM metrics_history
WHERE metric_id = :metric_id
AND timestamp = :timestamp;

-- name: query-sum-by-range
SELECT SUM(value) FROM metrics_history
WHERE metric_id = :metric_id
AND timestamp > :start
AND timestamp < :end;
