-- Esquema equivalente al que creaba SQLAlchemy en el backend FastAPI.
-- Se usa CREATE TABLE IF NOT EXISTS + baseline-on-migrate para poder reutilizar
-- una base de datos creada previamente por FastAPI.

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    created_at    TIMESTAMP,
    last_login    TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_users_username ON users (username);

CREATE TABLE IF NOT EXISTS tasks (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user        VARCHAR(255) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_tasks_user ON tasks (user);
