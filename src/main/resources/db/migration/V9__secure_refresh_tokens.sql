-- Store only a one-way digest of a refresh token. Existing issued tokens remain
-- usable after this migration because their digest is calculated before the raw
-- column is removed.
ALTER TABLE refresh_tokens
  ADD COLUMN token_hash CHAR(64) NULL AFTER token;

UPDATE refresh_tokens
SET token_hash = LOWER(SHA2(token, 256));

ALTER TABLE refresh_tokens
  DROP INDEX uk_refresh_tokens_token,
  DROP COLUMN token,
  MODIFY COLUMN token_hash CHAR(64) NOT NULL,
  ADD UNIQUE KEY uk_refresh_tokens_token_hash (token_hash);
