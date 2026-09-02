ALTER TABLE posts
  ADD COLUMN admin_inquiry BOOLEAN NOT NULL DEFAULT FALSE AFTER category,
  ADD COLUMN is_secret BOOLEAN NOT NULL DEFAULT FALSE AFTER admin_inquiry,
  ADD KEY idx_posts_admin_inquiry_created (admin_inquiry, created_at);
