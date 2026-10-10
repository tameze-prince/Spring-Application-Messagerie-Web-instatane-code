-- Keep authentication identifiers unique regardless of letter casing.
-- The application normalizes email to lowercase and treats usernames case-insensitively.
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (LOWER(email));

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_lower
    ON users (LOWER(username));
