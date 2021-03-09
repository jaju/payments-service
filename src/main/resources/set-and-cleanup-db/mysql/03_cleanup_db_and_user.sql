-- Delete the database
drop database payments;

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

-- Drop the user,
-- NOTE: Don't use delete from mysql.user where user = 'springuser';
DROP USER 'paymentsuser'@'%';

FLUSH PRIVILEGES;

select user, host from mysql.user;
-- +------------------+-----------+
-- | user             | host      |
-- +------------------+-----------+
-- | mysql.infoschema | localhost |
-- | mysql.session    | localhost |
-- | mysql.sys        | localhost |
-- | root             | localhost |
-- +------------------+-----------+
