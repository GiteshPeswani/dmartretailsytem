-- V5__update_entity_type_enums_for_batches.sql
-- Allow BATCH in entity_type enum for audit_events and blockchain_transactions

ALTER TABLE audit_events MODIFY COLUMN entity_type ENUM('PRODUCT', 'ORDER', 'BATCH') NOT NULL;
ALTER TABLE blockchain_transactions MODIFY COLUMN entity_type ENUM('PRODUCT', 'ORDER', 'BATCH') NOT NULL;
