-- Knowledge base ownership + visibility. Two columns:
--   creator_id  — the user who created the KB. NULL for pre-existing rows
--                 (they predate ownership, so they are treated as public).
--   visibility  — 'PUBLIC' (shared with the whole workspace) or 'PRIVATE'
--                 (visible only to creator_id).
-- Existing KBs default to PUBLIC so their current workspace-wide visibility
-- is preserved exactly as before this column existed.

ALTER TABLE mate_wiki_knowledge_base ADD COLUMN IF NOT EXISTS creator_id BIGINT;
ALTER TABLE mate_wiki_knowledge_base ADD COLUMN IF NOT EXISTS visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';
CREATE INDEX IF NOT EXISTS idx_wiki_kb_creator ON mate_wiki_knowledge_base(creator_id);
