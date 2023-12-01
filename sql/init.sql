create database demodb;
\c demodb;

create table if not exists jobs
(
    id      bigserial primary key,
    title   text not null,
    url     text not null,
    company text not null
);

create table if not exists persons
(
    id   bigserial primary key,
    name text not null,
    age  int  not null
);
