-- Active: 1701644914065@@127.0.0.1@5432@reviewboard
create database reviewboard;

\c reviewboard;

CREATE TABLE IF NOT EXISTS companies (
        id bigserial primary key,
        slug TEXT UNIQUE NOT NULL,
        name TEXT UNIQUE NOT NULL,
        url TEXT UNIQUE NOT NULL,
        location TEXT,
        country TEXT,
        industry TEXT,
        image TEXT,
        tags TEXT []
    );