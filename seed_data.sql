-- Seed data · Airtime Loan Platform Wave 1 · EXAMPLE DATA ONLY
-- Run after schema.sql. Illustrative units from HLD Figure D14 (not a market price).
-- Scenario: one subscriber, two live advances, one recharge of 150 swept at 40% = 60,
-- allocated nested: A fee 7.50 -> A principal 50.00 -> B fee 2.50.
-- mandate_share 0.40 is example data; product max_share remains UNSET in config.

USE loan_mgmt;

SET @adv_a  = UUID_TO_BIN('01928a00-0000-7000-8000-00000000000a');
SET @adv_b  = UUID_TO_BIN('01928a00-0000-7000-8000-00000000000b');
SET @dis_a  = UUID_TO_BIN('01928a00-0000-7000-8000-0000000000da');
SET @dis_b  = UUID_TO_BIN('01928a00-0000-7000-8000-0000000000db');
SET @man    = UUID_TO_BIN('01928a00-0000-7000-8000-0000000000ee');
SET @intent = UUID_TO_BIN('01928a00-0000-7000-8000-000000000101');
SET @res1   = UUID_TO_BIN('01928a00-0000-7000-8000-000000000201');
SET @res2   = UUID_TO_BIN('01928a00-0000-7000-8000-000000000202');
SET @att    = UUID_TO_BIN('01928a00-0000-7000-8000-000000000301');
SET @post   = UUID_TO_BIN('01928a00-0000-7000-8000-000000000401');
SET @sub    = 'SUB-TOKEN-0001';
SET @hash   = UNHEX(SHA2('example-msisdn-0001', 256));

-- Disclosures (acknowledged)
INSERT INTO disclosure (disclosure_id, subscriber_ref, advance_id, pid_version, amount, fee, total_repayable, rendered_text, ack_token, acknowledged_at) VALUES
(@dis_a, @sub, @adv_a, 3, 50.0000, 7.5000, 57.5000, 'Airtime advance 50.00. Fee 7.50. Total to repay 57.50 from future recharges. Reply 1 to accept.', 'ACK-A-0001', '2026-10-01 09:00:05.000000'),
(@dis_b, @sub, @adv_b, 3, 30.0000, 5.0000, 35.0000, 'Airtime advance 30.00. Fee 5.00. Total to repay 35.00 from future recharges. Reply 1 to accept.', 'ACK-B-0001', '2026-10-05 18:30:05.000000');

-- Mandate (one keystroke, two records)
INSERT INTO mandate (mandate_id, subscriber_ref, disclosure_id, mandate_share, status, armed_at) VALUES
(@man, @sub, @dis_a, 0.400000, 'ACTIVE', '2026-10-01 09:00:05.000000');

-- Advances (state AFTER the sweep below: A recovered, B partially recovered)
INSERT INTO advance_agreement (advance_id, market_code, subscriber_ref, msisdn_hash, msisdn_valid_from, origination_key, product_type, sku_id, pid_id, pid_version, currency, lender_of_record, regulated_flag, principal_amount, fee_amount, fee_method, principal_residual, fee_residual, status, drawn_at, due_date, grace_datetime) VALUES
(@adv_a, 'DEV', @sub, @hash, '2025-01-01 00:00:00', 'ORIG-0001-A', 'AIRTIME', 'SKU-AIRTIME-50', 'PID-AIRTIME', 3, 'XXX', 'TELCO-DEV', FALSE, 50.0000, 7.5000, 'FLAT', 0.0000, 0.0000, 'RECOVERED',           '2026-10-01 09:00:06.000000', '2026-10-15', '2026-10-22 23:59:59.000000'),
(@adv_b, 'DEV', @sub, @hash, '2025-01-01 00:00:00', 'ORIG-0001-B', 'AIRTIME', 'SKU-AIRTIME-30', 'PID-AIRTIME', 3, 'XXX', 'TELCO-DEV', FALSE, 30.0000, 5.0000, 'FLAT', 30.0000, 2.5000, 'PARTIALLY_RECOVERED', '2026-10-05 18:30:06.000000', '2026-10-19', '2026-10-26 23:59:59.000000');

INSERT INTO schedule_event (event_id, advance_id, event_type, payload, occurred_at) VALUES
(UUID_TO_BIN(UUID()), @adv_a, 'SCHEDULED', JSON_OBJECT('due_date','2026-10-15','grace_days',7), '2026-10-01 09:00:06.000000'),
(UUID_TO_BIN(UUID()), @adv_b, 'SCHEDULED', JSON_OBJECT('due_date','2026-10-19','grace_days',7), '2026-10-05 18:30:06.000000');

INSERT INTO dunning_state (advance_id, days_past_due, aging_bucket, ladder_step, updated_at) VALUES
(@adv_a, 0, 0, 0, '2026-10-10 12:00:02.000000'),
(@adv_b, 0, 0, 0, '2026-10-10 12:00:02.000000');

-- Recharge of 150 -> quote 60 (40%)
INSERT INTO deduction_intent (intent_id, signal_id, subscriber_ref, gross_recharge, quoted_amount, status, created_at) VALUES
(@intent, 'SIG-0001', @sub, 150.0000, 60.0000, 'SETTLED', '2026-10-10 12:00:00.000000');

INSERT INTO collection_reservation (reservation_id, signal_id, subscriber_ref, verb, amount, currency, created_at) VALUES
(@res1, 'SIG-0001', @sub, 'RESERVE', 60.0000, 'XXX', '2026-10-10 12:00:00.100000'),
(@res2, 'SIG-0001', @sub, 'CONSUME', 60.0000, 'XXX', '2026-10-10 12:00:02.000000');

INSERT INTO recovery_attempt (attempt_id, intent_id, outcome, conduct_record, created_at) VALUES
(@att, @intent, 'SETTLED', JSON_OBJECT('mandate_status','ACTIVE','dispute_hold',false,'mandate_share',0.40,'gross',150,'collectable',92.5,'quoted',60,'source','PAID_TOPUP'), '2026-10-10 12:00:02.000000');

-- Posting at settlement
INSERT INTO payment_posting (posting_id, idempotency_key, subscriber_ref, source, signal_id, amount, currency, authorised, settled_at) VALUES
(@post, 'SIG-0001:SETTLE', @sub, 'RECHARGE_SWEEP', 'SIG-0001', 60.0000, 'XXX', TRUE, '2026-10-10 12:00:02.000000');

-- Nested allocation
INSERT INTO allocation (allocation_id, posting_id, advance_id, component, amount, seq, created_at) VALUES
(UUID_TO_BIN(UUID()), @post, @adv_a, 'FEE',       7.5000,  1, '2026-10-10 12:00:02.000000'),
(UUID_TO_BIN(UUID()), @post, @adv_a, 'PRINCIPAL', 50.0000, 2, '2026-10-10 12:00:02.000000'),
(UUID_TO_BIN(UUID()), @post, @adv_b, 'FEE',       2.5000,  3, '2026-10-10 12:00:02.000000');

-- Outbox events (one published, one pending)
INSERT INTO outbox (topic, msg_key, event_type, payload, created_at, published_at) VALUES
('accounting.events', BIN_TO_UUID(@adv_a), 'FEE_BOOKED', JSON_OBJECT('advance_id',BIN_TO_UUID(@adv_a),'fee','7.5000','currency','XXX'), '2026-10-01 09:00:06.000000', '2026-10-01 09:00:06.200000'),
('loan.events',       BIN_TO_UUID(@adv_a), 'ADVANCE_RECOVERED', JSON_OBJECT('advance_id',BIN_TO_UUID(@adv_a)), '2026-10-10 12:00:02.000000', NULL);

USE reports;

SET @j_a = UUID_TO_BIN('01928a00-0000-7000-8000-000000000501');
SET @j_b = UUID_TO_BIN('01928a00-0000-7000-8000-000000000502');

-- Fee revenue journals (balanced double entry)
INSERT INTO journal (journal_id, source_event_id, advance_id, journal_type, market_code, currency, posted_at) VALUES
(@j_a, 'EVT-FEE-A', @adv_a, 'FEE_REVENUE', 'DEV', 'XXX', '2026-10-01 09:00:07.000000'),
(@j_b, 'EVT-FEE-B', @adv_b, 'FEE_REVENUE', 'DEV', 'XXX', '2026-10-05 18:30:07.000000');

INSERT INTO journal_line (line_id, journal_id, account_code, side, amount) VALUES
(UUID_TO_BIN(UUID()), @j_a, 'FEE_RECEIVABLE', 'DR', 7.5000),
(UUID_TO_BIN(UUID()), @j_a, 'FEE_INCOME',     'CR', 7.5000),
(UUID_TO_BIN(UUID()), @j_b, 'FEE_RECEIVABLE', 'DR', 5.0000),
(UUID_TO_BIN(UUID()), @j_b, 'FEE_INCOME',     'CR', 5.0000);

-- Hash chain (genesis prev_hash = 32 zero bytes)
SET @zero = UNHEX(REPEAT('00', 32));
SET @p1 = UNHEX(SHA2('RESERVE|SIG-0001|60.0000', 256));
SET @c1 = UNHEX(SHA2(CONCAT(HEX(@zero), HEX(@p1)), 256));
SET @p2 = UNHEX(SHA2('CONSUME|SIG-0001|60.0000', 256));
SET @c2 = UNHEX(SHA2(CONCAT(HEX(@c1), HEX(@p2)), 256));
INSERT INTO audit_chain (entity_type, entity_id, action, payload_hash, prev_hash, chain_hash, created_at) VALUES
('RESERVATION', 'SIG-0001', 'RESERVE', @p1, @zero, @c1, '2026-10-10 12:00:00.100000'),
('RESERVATION', 'SIG-0001', 'CONSUME', @p2, @c1,   @c2, '2026-10-10 12:00:02.000000');

INSERT INTO recon_window (window_id, window_start, window_end, quoted_total, ingested_total, posted_total, filtered_count, break_count, exception_owner) VALUES
(UUID_TO_BIN(UUID()), '2026-10-10 12:00:00', '2026-10-10 13:00:00', 60.0000, 60.0000, 60.0000, 13, 0, 'recon-ops');

INSERT INTO billed_vs_collected (rail, business_date, billed, collected) VALUES
('RECHARGE_SWEEP', '2026-10-10', 60.0000, 60.0000);

INSERT INTO projection_watermark (projection_name, topic, partition_no, last_offset, updated_at) VALUES
('account_360', 'loan.events',       0, 41, '2026-10-10 12:00:03.000000'),
('account_360', 'accounting.events', 0, 17, '2026-10-10 12:00:03.000000');
