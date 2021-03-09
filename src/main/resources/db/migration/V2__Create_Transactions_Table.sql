CREATE TABLE `transactions` (
  `id` binary(255) NOT NULL,
  `date` datetime DEFAULT NULL,
  `order_id` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `amount` double DEFAULT NULL,
  `currency` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- OLD using String as UUID
--CREATE TABLE `transactions` (
--  `id` varchar(255) NOT NULL,
--  `date` datetime DEFAULT NULL,
--  `order_id` varchar(255) DEFAULT NULL,
--  `status` varchar(255) DEFAULT NULL,
--  `amount` double DEFAULT NULL,
--  `currency` varchar(255) DEFAULT NULL,
--  PRIMARY KEY (`id`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--------------------------------------------------------------------
-- UNDO Script
--------------------------------------------------------------------
-- DROP TABLE `transactions`;
-- DELETE FROM flyway_schema_history WHERE installed_rank=1;