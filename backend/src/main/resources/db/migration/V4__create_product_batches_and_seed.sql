-- V4__create_product_batches_and_seed.sql
-- Create product batches table for supply chain tracking

CREATE TABLE IF NOT EXISTS product_batches (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    batch_number            VARCHAR(100) NOT NULL,
    product_id              BIGINT UNSIGNED NOT NULL,
    quantity                INT NOT NULL DEFAULT 100,
    manufacture_date        DATE NOT NULL,
    expiry_date             DATE NOT NULL,
    stage                   VARCHAR(30) NOT NULL DEFAULT 'MANUFACTURED',
    warehouse_received_at   DATETIME(6) NULL,
    store_received_at       DATETIME(6) NULL,
    blockchain_batch_id     VARCHAR(66) NULL,
    blockchain_tx_hash      VARCHAR(66) NULL,
    blockchain_status       VARCHAR(20) NOT NULL DEFAULT 'NOT_REGISTERED',
    data_hash               VARCHAR(66) NULL,
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_product_batches_num UNIQUE (batch_number),
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed initial test batches
INSERT IGNORE INTO product_batches (id, batch_number, product_id, quantity, manufacture_date, expiry_date, stage, warehouse_received_at, store_received_at, blockchain_status, data_hash)
VALUES
(1, 'BATCH-2026-MILK-01', 1, 150, '2026-10-15', '2026-11-15', 'STORE_ARRIVED', '2026-10-16 08:30:00', '2026-10-17 10:00:00', 'CONFIRMED', '0x833153e2050e3a00c53c6ec0e67dabb2a019f296151f8ef4324ef4078856596b'),
(2, 'BATCH-2026-COLG-02', 16, 200, '2026-09-01', '2028-09-01', 'WAREHOUSE_ARRIVED', '2026-09-05 14:15:00', NULL, 'CONFIRMED', '0xe601079a4dd8790ed721a7aebf837d3e0de2514dfbf6be7cc18d010527d8bc1a'),
(3, 'BATCH-2026-RICE-03', 2, 500, '2026-10-01', '2027-10-01', 'MANUFACTURED', NULL, NULL, 'CONFIRMED', '0x6a6fd3bcca5e1005f116b9718e1524336db679d2736ce04fadf1fd79aca262b2');
