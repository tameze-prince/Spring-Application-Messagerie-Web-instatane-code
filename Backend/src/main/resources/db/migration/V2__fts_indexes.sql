-- PostgreSQL Full Text Search GIN Indexes

-- Add tsvector search column for messages or GIN expression index
CREATE INDEX idx_messages_body_fts ON messages USING GIN (to_tsvector('english', coalesce(body, '')));

-- Add tsvector search index for users (username, first_name, last_name)
CREATE INDEX idx_users_search_fts ON users USING GIN (
    to_tsvector('english', coalesce(username, '') || ' ' || coalesce(first_name, '') || ' ' || coalesce(last_name, ''))
);

-- Add tsvector search index for conversations (title, description)
CREATE INDEX idx_conversations_search_fts ON conversations USING GIN (
    to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(username, ''))
);
