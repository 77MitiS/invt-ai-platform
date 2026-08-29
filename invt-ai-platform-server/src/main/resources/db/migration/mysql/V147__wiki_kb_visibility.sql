-- Knowledge base ownership + visibility. See the h2 sibling for the prose
-- explanation. MySQL lacks `ADD COLUMN IF NOT EXISTS` and
-- `CREATE INDEX IF NOT EXISTS`, so both are guarded with INFORMATION_SCHEMA
-- checks + prepared statements (idempotent).

SET @c := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'mate_wiki_knowledge_base'
             AND COLUMN_NAME = 'creator_id');
SET @s := IF(@c = 0,
    'ALTER TABLE mate_wiki_knowledge_base ADD COLUMN creator_id BIGINT',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'mate_wiki_knowledge_base'
             AND COLUMN_NAME = 'visibility');
SET @s := IF(@c = 0,
    'ALTER TABLE mate_wiki_knowledge_base ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT ''PUBLIC''',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @i := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'mate_wiki_knowledge_base'
             AND INDEX_NAME = 'idx_wiki_kb_creator');
SET @s := IF(@i = 0,
    'CREATE INDEX idx_wiki_kb_creator ON mate_wiki_knowledge_base(creator_id)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
