-- Airtime Loan Platform · Wave 1 · MySQL 8 InnoDB
-- Schemas:reports (Reports)
-- Draft from ATL-HLD-001. UNSET config values are NOT hardcoded here.
-- Money: DECIMAL(19,4) minor-unit-safe; currency pinned from pid_version at draw.
-- Monthly RANGE partitions on hot money tables (posting, allocation, reservation, attempt).

CREATE DATABASE IF NOT EXISTS reports   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE reports;

CREATE TABLE journal (
  journal_id      BINARY(16)   NOT NULL,
  source_event_id VARCHAR(128) NOT NULL,     -- from accounting.events; replay-safe
  advance_id      BINARY(16)   NOT NULL,
  journal_type    VARCHAR(32)  NOT NULL,     -- FEE_REVENUE|WRITE_OFF|RECOVERY_OF_WRITTEN_OFF
  market_code     VARCHAR(8)   NOT NULL,
  currency        CHAR(3)      NOT NULL,
  posted_at       DATETIME(6)  NOT NULL,
  PRIMARY KEY (journal_id),
  UNIQUE KEY uk_source (source_event_id)
) ENGINE=InnoDB;

CREATE TABLE journal_line (
  line_id       BINARY(16)    NOT NULL,
  journal_id    BINARY(16)    NOT NULL,
  account_code  VARCHAR(32)   NOT NULL,      -- chart of accounts lives here, not in LoanManagement
  side          CHAR(2)       NOT NULL,      -- DR|CR
  amount        DECIMAL(19,4) NOT NULL,
  PRIMARY KEY (line_id),
  KEY ix_journal (journal_id),
  CONSTRAINT ck_side CHECK (side IN ('DR','CR'))
) ENGINE=InnoDB;

CREATE TABLE audit_chain (
  seq          BIGINT       NOT NULL AUTO_INCREMENT,
  entity_type  VARCHAR(32)  NOT NULL,
  entity_id    VARCHAR(64)  NOT NULL,
  action       VARCHAR(32)  NOT NULL,        -- incl. RESERVE/CONSUME/RELEASE
  payload_hash BINARY(32)   NOT NULL,
  prev_hash    BINARY(32)   NOT NULL,
  chain_hash   BINARY(32)   NOT NULL,        -- SHA-256(prev_hash || payload_hash)
  created_at   DATETIME(6)  NOT NULL,
  PRIMARY KEY (seq)
) ENGINE=InnoDB;

CREATE TABLE recon_window (
  window_id           BINARY(16)    NOT NULL,
  window_start        DATETIME(6)   NOT NULL,
  window_end          DATETIME(6)   NOT NULL,
  quoted_total        DECIMAL(19,4) NOT NULL,
  ingested_total      DECIMAL(19,4) NOT NULL,
  posted_total        DECIMAL(19,4) NOT NULL,
  filtered_count      BIGINT        NOT NULL,  -- counted, not rowed
  break_count         INT           NOT NULL,
  exception_owner     VARCHAR(64)   NULL,
  PRIMARY KEY (window_id)
) ENGINE=InnoDB;

CREATE TABLE billed_vs_collected (
  rail          VARCHAR(32)   NOT NULL,
  business_date DATE          NOT NULL,
  billed        DECIMAL(19,4) NOT NULL,
  collected     DECIMAL(19,4) NOT NULL,
  PRIMARY KEY (rail, business_date)
) ENGINE=InnoDB;

CREATE TABLE projection_watermark (
  projection_name VARCHAR(64) NOT NULL,
  topic           VARCHAR(64) NOT NULL,
  partition_no    INT         NOT NULL,
  last_offset     BIGINT      NOT NULL,
  updated_at      DATETIME(6) NOT NULL,
  PRIMARY KEY (projection_name, topic, partition_no)
) ENGINE=InnoDB;
