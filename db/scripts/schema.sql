-- Database: schema

-- DROP DATABASE IF EXISTS schema;

CREATE DATABASE schema
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'Russian_Russia.1251'
    LC_CTYPE = 'Russian_Russia.1251'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

create table rabbit(
      id serial primary key,
      created_date TIMESTAMP
);

create table post(
	id serial primary key,
	name TEXT,
	text TEXT,
	link TEXT UNIQUE,
	created TIMESTAMP
);