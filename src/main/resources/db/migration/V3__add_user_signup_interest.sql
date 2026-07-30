ALTER TABLE users
  ADD COLUMN signup_interest VARCHAR(20) NULL COMMENT 'Signup interest: ECONOMY / POLICY / SAVING'
  AFTER region;
