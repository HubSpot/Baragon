--liquibase formatted sql

--changeset ssalinas:1 dbms:mysql
CREATE TABLE `responseHistory` (
  `requestId` varchar(200) NOT NULL,
  `serviceId` varchar(100) NOT NULL,
  `bytes` blob NOT NULL,
  `createdAt` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`requestId`),
  KEY `serviceId` (`serviceId`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;