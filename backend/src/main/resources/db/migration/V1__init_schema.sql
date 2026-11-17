-- V1__init_schema.sql
-- D-Mart Blockchain: Initial Schema

CREATE TABLE IF NOT EXISTS roles (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(30) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    phone          VARCHAR(20),
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id  BIGINT UNSIGNED NOT NULL,
    role_id  BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS addresses (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT UNSIGNED NOT NULL,
    label        VARCHAR(50),
    street       VARCHAR(255) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20) NOT NULL,
    country      VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sku                     VARCHAR(100) NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    category_id             BIGINT UNSIGNED NOT NULL,
    price                   DECIMAL(12,2) NOT NULL,
    discount_percent        DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    stock_quantity          INT NOT NULL DEFAULT 0,
    image_url               VARCHAR(500),
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    blockchain_product_id   VARCHAR(66),
    blockchain_tx_hash      VARCHAR(66),
    blockchain_status       ENUM('PENDING','CONFIRMED','FAILED','NOT_REGISTERED') NOT NULL DEFAULT 'NOT_REGISTERED',
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_products_category  ON products(category_id);
CREATE INDEX idx_products_active    ON products(active);
CREATE INDEX idx_products_bc_status ON products(blockchain_status);

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_number        VARCHAR(50) NOT NULL,
    customer_id         BIGINT UNSIGNED NOT NULL,
    delivery_address    TEXT NOT NULL,
    status              ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(12,2) NOT NULL,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(12,2) NOT NULL,
    notes               TEXT,
    blockchain_order_id VARCHAR(66),
    blockchain_tx_hash  VARCHAR(66),
    blockchain_status   ENUM('PENDING','CONFIRMED','FAILED','NOT_SUBMITTED') NOT NULL DEFAULT 'NOT_SUBMITTED',
    order_hash          VARCHAR(66),
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_orders_customer   ON orders(customer_id);
CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_orders_bc_status  ON orders(blockchain_status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity    INT NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    line_total  DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(id)   ON DELETE CASCADE,
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_oi_order   ON order_items(order_id);
CREATE INDEX idx_oi_product ON order_items(product_id);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT UNSIGNED NOT NULL,
    user_id      BIGINT UNSIGNED,
    order_id     BIGINT UNSIGNED,
    movement_type ENUM('PURCHASE','SALE','ADJUSTMENT','RETURN','DAMAGED') NOT NULL,
    quantity_change INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after  INT NOT NULL,
    reason       VARCHAR(500),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_inv_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_inv_order   FOREIGN KEY (order_id)   REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_inv_product ON inventory_movements(product_id);
CREATE INDEX idx_inv_created ON inventory_movements(created_at);

CREATE TABLE IF NOT EXISTS blockchain_transactions (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tx_hash          VARCHAR(66),
    entity_type      ENUM('PRODUCT','ORDER') NOT NULL,
    entity_id        BIGINT UNSIGNED NOT NULL,
    event_type       VARCHAR(50) NOT NULL,
    status           ENUM('PENDING','CONFIRMED','FAILED') NOT NULL DEFAULT 'PENDING',
    block_number     BIGINT,
    gas_used         BIGINT,
    error_message    TEXT,
    retry_count      INT NOT NULL DEFAULT 0,
    submitted_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at     DATETIME(6),
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bc_tx_hash     ON blockchain_transactions(tx_hash);
CREATE INDEX idx_bc_entity      ON blockchain_transactions(entity_type, entity_id);
CREATE INDEX idx_bc_status      ON blockchain_transactions(status);

CREATE TABLE IF NOT EXISTS audit_events (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    entity_type   ENUM('PRODUCT','ORDER') NOT NULL,
    entity_id     BIGINT UNSIGNED NOT NULL,
    event_type    VARCHAR(50) NOT NULL,
    actor_id      BIGINT UNSIGNED,
    actor_role    VARCHAR(30),
    description   VARCHAR(500),
    bc_tx_id      BIGINT UNSIGNED,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ae_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT fk_ae_bc_tx FOREIGN KEY (bc_tx_id) REFERENCES blockchain_transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ae_entity  ON audit_events(entity_type, entity_id);
CREATE INDEX idx_ae_created ON audit_events(created_at);
