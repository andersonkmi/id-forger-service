create table series_details (
    name varchar(255) primary key,
    sequence varchar(255) not null,
    prefix varchar(255) not null,
    created_at timestamp with time zone not null default now()
);