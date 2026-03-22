CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS search_documents (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL,
    entity_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT '',
    content_text TEXT NOT NULL DEFAULT '',
    content_tsv tsvector GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content_text, ''))
    ) STORED,
    embedding vector(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_search_documents_tsv
    ON search_documents USING GIN (content_tsv);

CREATE INDEX IF NOT EXISTS idx_search_documents_embedding
    ON search_documents USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_search_documents_type_updated
    ON search_documents (entity_type, updated_at DESC);

INSERT INTO search_documents (entity_type, entity_id, title, content_text, updated_at)
SELECT
    'EVENT',
    e.id,
    coalesce(e.title, ''),
    trim(concat_ws(' ', e.description, e.venue, e.ai_description, e.status::text)),
    now()
FROM events e
ON CONFLICT (entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    content_text = EXCLUDED.content_text,
    updated_at = now();

INSERT INTO search_documents (entity_type, entity_id, title, content_text, updated_at)
SELECT
    'USER',
    u.id,
    coalesce(u.username, ''),
    trim(concat_ws(' ', u.email, u.role)),
    now()
FROM users u
ON CONFLICT (entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    content_text = EXCLUDED.content_text,
    updated_at = now();

