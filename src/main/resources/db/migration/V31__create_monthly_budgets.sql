CREATE TABLE monthly_budgets (
  monthly_budget_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id           BIGINT UNSIGNED NOT NULL,
  budget_month      DATE            NOT NULL,
  amount            INT UNSIGNED    NOT NULL,
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (monthly_budget_id),
  UNIQUE KEY uk_monthly_budgets_user_month (user_id, budget_month),
  CONSTRAINT fk_monthly_budgets_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) COMMENT '월간 예산';
