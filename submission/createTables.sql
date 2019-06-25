-- add all your SQL setup statements here. 

-- You can assume that the following base table has been created with data loaded for you when we test your submission 
-- (you still need to create and populate it in your instance however),
-- although you are free to insert extra ALTER COLUMN ... statements to change the column 
-- names / types if you like.

-- CREATE TABLE FLIGHTS
-- (
--  fid int NOT NULL PRIMARY KEY,
--  year int,
--  month_id int,
--  day_of_month int,
--  day_of_week_id int,
--  carrier_id varchar(3),
--  flight_num int,
--  origin_city varchar(34),
--  origin_state varchar(47),
--  dest_city varchar(34),
--  dest_state varchar(46),
--  departure_delay double precision,
--  taxi_out double precision,
--  arrival_delay double precision,
--  canceled int,
--  actual_time double precision,
--  distance double precision,
--  capacity int,
--  price double precision
--)
drop table if exists Users;
create table Users(
    username VARCHAR(20) primary key,
    password Varchar(20),
    initAmount int
);
select * from Users;
drop table if exists Itineraries;
create table Itineraries(
    id int primary key ,
    itinum1 int,
    itinum2 int
);

drop table if exists reservations;
create table reservations(
    uid int primary Key,
    username varchar(20),
    fid1 int,
    fid2 int,
    day_of_month int,
    ifPaid int,
    fid1Price int,
    fid2Price int,
    totalprice int,
    fid1Carrier varchar(10),
    fid2Carrier varchar(10),
    fid1Num int,
    fid2Num int,
    fid1Origin varchar(30),
    fid2Origin varchar(30),
    fid1Dest varchar(30),
    fid2Dest varchar(30),
    fid1Duration int,
    fid2Duration int,
    fid1Capacity int,
    fid2Capacity int

);
