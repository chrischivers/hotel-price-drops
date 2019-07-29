CREATE TABLE IF NOT EXISTS users (
     id SERIAL PRIMARY KEY,
     email_address VARCHAR NOT NULL,
     search_id INT NOT NULL,
     CONSTRAINT users_constraint UNIQUE (email_address, search_id)
);

ALTER TABLE users
ADD FOREIGN KEY (search_id)
REFERENCES searches(id);