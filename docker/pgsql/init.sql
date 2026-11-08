create sequence idfs_default;
create sequence idfs_product
    start with 1451000000
    minvalue 1451000000
    maxvalue 1451999999
    increment by 1;
create table series_details (
                                name varchar(255) primary key,
                                sequence varchar(255) not null,
                                prefix varchar(255) not null,
                                created_at timestamp with time zone not null default now()
);