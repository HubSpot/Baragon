--liquibase formatted sql

--changeset ssalinas:1 dbms:mysql
CREATE TABLE `responseHistory` (
  `requestId` varchar(200) NOT NULL,
  `serviceId` varchar(100) NOT NULL,
  `bytes` blob NOT NULL,
  `updatedAt` timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  PRIMARY KEY (`requestId`),
  KEY `serviceId` (`serviceId`, `updatedAt`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;