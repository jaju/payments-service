show databases;
-- +--------------------+
-- | Database           |
-- +--------------------+
-- | information_schema |
-- | mysql              |
-- | performance_schema |
-- | sys                |
-- +--------------------+

-- Creates the new database
create database payments;

show databases;
-- +--------------------+
-- | Database           |
-- +--------------------+
-- | payments           |
-- | information_schema |
-- | mysql              |
-- | performance_schema |
-- | sys                |
-- +--------------------+

select user, host from mysql.user;
-- +------------------+-----------+
-- | user             | host      |
-- +------------------+-----------+
-- | mysql.infoschema | localhost |
-- | mysql.session    | localhost |
-- | mysql.sys        | localhost |
-- | root             | localhost |
-- +------------------+-----------+

-- Creates the user
create user 'paymentsuser'@'%' identified by 'PaymentsPassword';

select user, host from mysql.user;
-- +------------------+-----------+
-- | user             | host      |
-- +------------------+-----------+
-- | paymentsuser     | %         |
-- | mysql.infoschema | localhost |
-- | mysql.session    | localhost |
-- | mysql.sys        | localhost |
-- | root             | localhost |
-- +------------------+-----------+

-- Gives all privileges to the new user on the newly created database
grant all on payments.* to 'paymentsuser'@'%';

FLUSH PRIVILEGES;