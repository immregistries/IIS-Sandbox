CREATE TABLE org_location
(
  org_location_id         INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
  org_facility_code       VARCHAR(250)   NOT NULL,
  org_id                  INT            NOT NULL,
  org_facility_name       VARCHAR(250),
  location_type           VARCHAR(250),
  address_line1           VARCHAR(250),
  address_line2           VARCHAR(250),
  address_city            VARCHAR(250),
  address_state           VARCHAR(250),
  address_zip             VARCHAR(250),
  address_country         VARCHAR(250),
  address_county_parish   VARCHAR(250)
);

ALTER TABLE vaccination_reported ADD COLUMN org_location_id INT;
