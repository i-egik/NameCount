CREATE DATABASE named_count ENCODING 'UTF-8';
\c named_count
CREATE SCHEMA properties;
SET search_path='properties';
CREATE TABLE counter_catalog
(
  id          SERIAL PRIMARY KEY,
  name        VARCHAR   NOT NULL,
  description VARCHAR   NOT NULL,
  created     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (name)
);

CREATE TABLE counter_values
(
  id         SERIAL PRIMARY KEY,
  counter_id BIGINT    NOT NULL REFERENCES counter_catalog (id),
  user_id    BIGINT    NOT NULL,
  value      BIGINT    NOT NULL,
  created    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (counter_id, user_id)
);
