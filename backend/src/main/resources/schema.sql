CREATE TABLE IF NOT EXISTS members (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    nickname TEXT NOT NULL,
    password_hash TEXT NOT NULL
);
