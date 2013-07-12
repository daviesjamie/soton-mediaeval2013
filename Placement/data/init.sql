-- CREATE DATABASE Placement;
-- USE Placement;

CREATE TABLE image_metadata (
    photoID BIGINT UNSIGNED PRIMARY KEY,
    accuracy TINYINT UNSIGNED,
    userID VARCHAR(15),
    photoLink VARCHAR(100),
    photoTags VARCHAR(300),
    dateTaken INT,
    dateUploaded INT,
    views INT UNSIGNED,
    licenseID TINYINT UNSIGNED
);

CREATE TABLE image_locations (
    photoID BIGINT UNSIGNED PRIMARY KEY,
    latitude DECIMAL(7,5),
    longitude DECIMAL(8,5)
) ENGINE=MYISAM;

CREATE TABLE country_polygons (
    countryName VARCHAR(50) PRIMARY KEY,
    polygon MULTIPOLYGON NOT NULL
) ENGINE=MYISAM;

CREATE SPATIAL INDEX country_polygon_index ON country_polygons(polygon);

-- Load in data from the CSV files
LOAD DATA LOCAL INFILE 'metadata_1.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_2.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_3.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_4.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_5.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_6.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_7.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_8.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'metadata_9.csv'
INTO TABLE image_metadata
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'training_latlng'
INTO TABLE image_locations
FIELDS TERMINATED BY ' '
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

-- Add Spatial column (and index) to image_locations table, now that data has been loaded
ALTER TABLE image_locations ADD coords POINT;
UPDATE image_locations SET coords = GeometryFromText( CONCAT( 'POINT(', latitude, ' ', longitude, ')' ) );
ALTER TABLE image_locations CHANGE coords coords POINT NOT NULL;
CREATE SPATIAL INDEX location_coords_index ON image_locations(coords);

-- Create a table for the testing set
CREATE TABLE vietnamese_images AS
SELECT photoID, latitude, longitude FROM image_locations
WHERE ST_Contains(
    (SELECT polygon FROM country_polygons WHERE countryName='Vietnam'), coords
) = 1;
