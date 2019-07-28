ALTER TABLE results
ADD FOREIGN KEY (search_id)
REFERENCES searches(id);

ALTER TABLE results
ADD FOREIGN KEY (hotel_id)
REFERENCES hotels(id);

CREATE INDEX results_search_id_idx
ON results(search_id);

CREATE INDEX results_hotel_id_idx
ON results(hotel_id);

CREATE INDEX results_timestamp_idx
ON results(timestamp);

CREATE INDEX results_lowest_price_idx
ON results(lowest_price);
