CREATE KEYSPACE IF NOT EXISTS db
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS db.weather_station (
  country TEXT,
  city TEXT,
  station_id TEXT,
  entry INT,
  value INT,
  PRIMARY KEY (country, city, station_id)
);