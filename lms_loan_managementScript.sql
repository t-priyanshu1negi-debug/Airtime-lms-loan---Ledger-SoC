-- Airtime Loan Platform · Wave 1 · MySQL 8 InnoDB
-- Schema: loan_mgmt (LoanManagement)
-- Draft from ATL-HLD-001. UNSET config values are NOT hardcoded here.
-- Money: DECIMAL(19,4) minor-unit-safe; currency pinned from pid_version at draw.
-- Monthly RANGE partitions on hot money tables (posting, allocation, reservation, attempt).

CREATE DATABASE IF NOT EXISTS loan_mgmt CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE loan_mgmt;

-- Advance agreement (bullet; one term, one due date, one grace window)
CREATE TABLE advance_agreement (
  advance_id           BINARY(16)    NOT NULL,           -- UUIDv7
  market_code          VARCHAR(8)    NOT NULL,
  subscriber_ref       VARCHAR(64)   NOT NULL,           -- tokenised; no raw MSISDN in logs
  msisdn_hash          BINARY(32)    NOT NULL,
  msisdn_valid_from    DATETIME(6)   NOT NULL,           -- bitemporal binding
  origination_key      VARCHAR(128)  NOT NULL,           -- number + origination idempotency
  product_type         VARCHAR(16)   NOT NULL,           -- AIRTIME|VOICE|DATA|COMBO|CUSTOM_COMBO
  sku_id               VARCHAR(64)   NOT NULL,
  pid_id               VARCHAR(64)   NOT NULL,
  pid_version          INT           NOT NULL,           -- pinned at draw
  currency             CHAR(3)       NOT NULL,           -- from pid_version
  lender_of_record     VARCHAR(64)   NOT NULL,           -- telco
  regulated_flag       BOOLEAN       NOT NULL,           -- classification snapshot at origination
  principal_amount     DECIMAL(19,4) NOT NULL,
  fee_amount           DECIMAL(19,4) NOT NULL,
  fee_method           VARCHAR(16)   NOT NULL,           -- FLAT|PERCENT|TIERED
  principal_residual   DECIMAL(19,4) NOT NULL,
  fee_residual         DECIMAL(19,4) NOT NULL,
  status               VARCHAR(24)   NOT NULL,           -- PENDING|DISBURSED|PARTIALLY_RECOVERED|RECOVERED|EXPIRED|WRITTEN_OFF|CANCELLED|ABORTED
  drawn_at             DATETIME(6)   NOT NULL,
  due_date             DATE          NOT NULL,
  grace_datetime       DATETIME(6)   NOT NULL,           -- due_date + grace_days (launch 7)
  written_off_at       DATETIME(6)   NULL,
  version              BIGINT        NOT NULL DEFAULT 0, -- optimistic lock
  created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (advance_id),
  UNIQUE KEY uk_origination (origination_key),
  KEY ix_sub_live (subscriber_ref, status, due_date, drawn_at),  -- nested waterfall order
  CONSTRAINT ck_status CHECK (status IN ('PENDING','DISBURSED','PARTIALLY_RECOVERED','RECOVERED','EXPIRED','WRITTEN_OFF','CANCELLED','ABORTED')),
  CONSTRAINT ck_residual CHECK (principal_residual >= 0 AND fee_residual >= 0)
) ENGINE=InnoDB;

-- Immutable schedule-event journal (one-row bullet + events)
CREATE TABLE schedule_event (
  event_id     BINARY(16)  NOT NULL,
  advance_id   BINARY(16)  NOT NULL,
  event_type   VARCHAR(32) NOT NULL,   -- SCHEDULED|GRACE_ENTERED|DPD_TICK|EXPIRED|WRITTEN_OFF
  payload      JSON        NULL,
  occurred_at  DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id),
  KEY ix_adv (advance_id, occurred_at)
) ENGINE=InnoDB;

CREATE TABLE disclosure (
  disclosure_id     BINARY(16)    NOT NULL,
  subscriber_ref    VARCHAR(64)   NOT NULL,
  advance_id        BINARY(16)    NULL,          -- linked after create
  pid_version       INT           NOT NULL,
  amount            DECIMAL(19,4) NOT NULL,
  fee               DECIMAL(19,4) NOT NULL,
  total_repayable   DECIMAL(19,4) NOT NULL,
  rendered_text     VARCHAR(182)  NOT NULL,      -- USSD budget; truncation = refusal
  ack_token         VARCHAR(128)  NOT NULL,
  acknowledged_at   DATETIME(6)   NULL,
  PRIMARY KEY (disclosure_id),
  UNIQUE KEY uk_ack (ack_token)
) ENGINE=InnoDB;

CREATE TABLE mandate (
  mandate_id      BINARY(16)   NOT NULL,
  subscriber_ref  VARCHAR(64)  NOT NULL,
  disclosure_id   BINARY(16)   NOT NULL,
  mandate_share   DECIMAL(7,6) NOT NULL,         -- narrower of this and max_share binds
  status          VARCHAR(16)  NOT NULL,         -- ARMED|ACTIVE|REVOKED
  armed_at        DATETIME(6)  NOT NULL,
  revoked_at      DATETIME(6)  NULL,
  PRIMARY KEY (mandate_id),
  KEY ix_sub (subscriber_ref, status)
) ENGINE=InnoDB;

CREATE TABLE dispute (
  dispute_id     BINARY(16)  NOT NULL,
  advance_id     BINARY(16)  NOT NULL,
  dispute_type   VARCHAR(24) NOT NULL,  -- NOT_REQUESTED|NOT_RECEIVED|WRONG_DEDUCTION|UNMANDATED
  status         VARCHAR(16) NOT NULL,  -- RAISED|UNDER_REVIEW|UPHELD|REJECTED|CLOSED
  hold_active    BOOLEAN     NOT NULL DEFAULT TRUE,  -- hold at RAISED; DPD continues
  raised_at      DATETIME(6) NOT NULL,
  resolved_at    DATETIME(6) NULL,
  PRIMARY KEY (dispute_id),
  KEY ix_adv_hold (advance_id, hold_active)
) ENGINE=InnoDB;

-- Collection reservation ledger (M23 partial; draft pending #19)
CREATE TABLE collection_reservation (
  reservation_id   BINARY(16)    NOT NULL,
  signal_id        VARCHAR(128)  NOT NULL,
  subscriber_ref   VARCHAR(64)   NOT NULL,
  verb             VARCHAR(8)    NOT NULL,   -- RESERVE|CONSUME|RELEASE (append-only rows)
  amount           DECIMAL(19,4) NOT NULL,
  currency         CHAR(3)       NOT NULL,
  refusal_code     VARCHAR(32)   NULL,
  created_at       DATETIME(6)   NOT NULL,
  PRIMARY KEY (reservation_id, created_at),
  UNIQUE KEY uk_signal_verb (signal_id, verb, created_at),
  CONSTRAINT ck_verb CHECK (verb IN ('RESERVE','CONSUME','RELEASE'))
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS (created_at) (
  PARTITION p2026_10 VALUES LESS THAN ('2026-11-01'),
  PARTITION p2026_11 VALUES LESS THAN ('2026-12-01'),
  PARTITION pmax     VALUES LESS THAN (MAXVALUE)
);

CREATE TABLE deduction_intent (
  intent_id       BINARY(16)    NOT NULL,
  signal_id       VARCHAR(128)  NOT NULL,
  subscriber_ref  VARCHAR(64)   NOT NULL,
  gross_recharge  DECIMAL(19,4) NOT NULL,
  quoted_amount   DECIMAL(19,4) NOT NULL,
  status          VARCHAR(16)   NOT NULL,  -- QUOTED|SETTLED|RELEASED|REFUSED
  created_at      DATETIME(6)   NOT NULL,
  PRIMARY KEY (intent_id),
  UNIQUE KEY uk_signal (signal_id)         -- exactly-once control
) ENGINE=InnoDB;

CREATE TABLE recovery_attempt (
  attempt_id      BINARY(16)   NOT NULL,
  intent_id       BINARY(16)   NOT NULL,
  outcome         VARCHAR(24)  NOT NULL,
  conduct_record  JSON         NOT NULL,   -- immutable; disputes adjudicated against it
  created_at      DATETIME(6)  NOT NULL,
  PRIMARY KEY (attempt_id, created_at),
  KEY ix_intent (intent_id)
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS (created_at) (
  PARTITION p2026_10 VALUES LESS THAN ('2026-11-01'),
  PARTITION pmax     VALUES LESS THAN (MAXVALUE)
);

-- Posting: post once at settlement; always credit borrower
CREATE TABLE payment_posting (
  posting_id       BINARY(16)    NOT NULL,
  idempotency_key  VARCHAR(128)  NOT NULL,   -- double-post control
  subscriber_ref   VARCHAR(64)   NOT NULL,
  source           VARCHAR(16)   NOT NULL,   -- RECHARGE_SWEEP|VOLUNTARY
  signal_id        VARCHAR(128)  NULL,
  amount           DECIMAL(19,4) NOT NULL,
  currency         CHAR(3)       NOT NULL,
  authorised       BOOLEAN       NOT NULL,   -- separate flag; late settlement still posts
  settled_at       DATETIME(6)   NOT NULL,
  PRIMARY KEY (posting_id, settled_at),
  UNIQUE KEY uk_idem (idempotency_key, settled_at)
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS (settled_at) (
  PARTITION p2026_10 VALUES LESS THAN ('2026-11-01'),
  PARTITION pmax     VALUES LESS THAN (MAXVALUE)
);
-- Nested allocation: earliest due -> fee -> principal, to the cap
CREATE TABLE allocation (
  allocation_id  BINARY(16)    NOT NULL,
  posting_id     BINARY(16)    NOT NULL,
  advance_id     BINARY(16)    NOT NULL,
  component      VARCHAR(12)   NOT NULL,   -- FEE|PRINCIPAL
  amount         DECIMAL(19,4) NOT NULL,
  seq            SMALLINT      NOT NULL,   -- waterfall order
  created_at     DATETIME(6)   NOT NULL,
  PRIMARY KEY (allocation_id, created_at),
  KEY ix_posting (posting_id, seq),
  KEY ix_adv (advance_id),
  CONSTRAINT ck_comp CHECK (component IN ('FEE','PRINCIPAL'))
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS (created_at) (
  PARTITION p2026_10 VALUES LESS THAN ('2026-11-01'),
  PARTITION pmax     VALUES LESS THAN (MAXVALUE)
);

CREATE TABLE dunning_state (
  advance_id       BINARY(16)  NOT NULL,
  days_past_due    INT         NOT NULL DEFAULT 0,
  aging_bucket     TINYINT     NOT NULL DEFAULT 0,   -- five buckets
  ladder_step      SMALLINT    NOT NULL DEFAULT 0,
  updated_at       DATETIME(6) NOT NULL,
  PRIMARY KEY (advance_id)
) ENGINE=InnoDB;

-- Transactional outbox
CREATE TABLE outbox (
  outbox_id     BIGINT       NOT NULL AUTO_INCREMENT,
  topic         VARCHAR(64)  NOT NULL,   -- loan.events, accounting.events, ...
  msg_key       VARCHAR(128) NOT NULL,
  event_type    VARCHAR(64)  NOT NULL,   -- UPPER_SNAKE
  payload       JSON         NOT NULL,
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  published_at  DATETIME(6)  NULL,
  PRIMARY KEY (outbox_id),
  KEY ix_unpublished (published_at, outbox_id)
) ENGINE=InnoDB;