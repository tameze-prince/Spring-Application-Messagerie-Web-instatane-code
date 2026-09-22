-- Spring Modulith Event Publication Table
CREATE TABLE event_publication (
    id UUID NOT NULL,
    completion_date TIMESTAMPTZ,
    event_type VARCHAR(255) NOT NULL,
    listener_id VARCHAR(255),
    publication_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    serialized_event TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_event_publication_completion_date ON event_publication (completion_date);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);