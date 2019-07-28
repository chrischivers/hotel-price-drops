CREATE TABLE IF NOT EXISTS hotels (
     id SERIAL PRIMARY KEY,
     hotel_name    VARCHAR NOT NULL,
     kayak_url    VARCHAR NULL,
     skyscanner_url   VARCHAR NULL,
     CONSTRAINT hotel_name_constraint UNIQUE (hotel_name)
);

CREATE TABLE IF NOT EXISTS searches (
     id SERIAL PRIMARY KEY,
     check_in_date    DATE NOT NULL,
     check_out_date    DATE NOT NULL,
     number_of_adults  SMALLINT NOT NULL,
     CONSTRAINT searches_constraint UNIQUE (check_in_date, check_out_date, number_of_adults)
);

CREATE TABLE IF NOT EXISTS results (
     id SERIAL PRIMARY KEY,
     search_id    INT NOT NULL,
     hotel_id    INT NOT NULL,
     lowest_price INT NULL,
     comparison_site_name VARCHAR,
     timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);