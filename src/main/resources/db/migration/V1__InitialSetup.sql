CREATE TABLE TODOS
(
    ID        BIGSERIAL PRIMARY KEY,
    TITLE     VARCHAR NOT NULL,
    COMPLETED BOOLEAN NOT NULL,
    ORDERING  INT
);


CREATE TABLE PROXY
(
    id      BIGSERIAL PRIMARY KEY,
    host    varchar not null,
    port    int     not null,
    country varchar(2) default null,
    level   int(4)     default null,
    rating  int default 0
)