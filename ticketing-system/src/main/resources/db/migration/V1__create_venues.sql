CREATE TABLE venues (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
