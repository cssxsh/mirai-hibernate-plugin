UPDATE `message_record` SET
    recall = 1
WHERE ids = null or ids = '';

UPDATE `message_record` SET
    recall = 2
WHERE recall = target_id;

UPDATE `message_record` SET
    recall = 3
WHERE recall > 12345;

ALTER TABLE `message_record`
CHANGE COLUMN `recall` `recall` TINYINT(4) NOT NULL DEFAULT 0 AFTER `kind`;