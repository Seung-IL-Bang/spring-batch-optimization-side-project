DROP TABLE IF EXISTS products;
CREATE TABLE products (
      product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
      seller_id BIGINT,
      product_name VARCHAR(255),
      product_category VARCHAR(50),
      tax_type VARCHAR(50),
      sell_price DECIMAL(19, 2) DEFAULT 0.00,
      supply_price DECIMAL(19, 2) DEFAULT 0.00,
      created_at DATETIME,
      updated_at DATETIME
);

DROP TABLE IF EXISTS seller;
CREATE TABLE seller (
    seller_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_name VARCHAR(50),
    business_no VARCHAR(255),
    default_delivery_amount DECIMAL(19, 2) DEFAULT 0.00,
    commission_rate DOUBLE,
    created_at DATETIME,
    updated_at DATETIME
);

DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    paid_pg_amount DECIMAL(19, 2) DEFAULT 0.00,
    promotion_discount_amount DECIMAL(19, 2) DEFAULT 0.00,
    point_used_amount DECIMAL(19, 2) DEFAULT 0.00,
    coupon_discount_amount DECIMAL(19, 2) DEFAULT 0.00,
    paid_confirmed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME
);

DROP TABLE IF EXISTS order_product;
CREATE TABLE order_product (
   order_product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
   order_id BIGINT,
   product_id BIGINT,
   quantity INT,
   delivery_status VARCHAR(50),
   purchase_confirmed_at DATETIME NULL,
   delivery_completed_at DATETIME NULL,
   created_at DATETIME,
   updated_at DATETIME
);

DROP TABLE IF EXISTS customer;
CREATE TABLE customer (
  customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255),
  name VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);

DROP TABLE IF EXISTS claim;
CREATE TABLE claim (
   claim_id BIGINT PRIMARY KEY AUTO_INCREMENT,
   customer_id BIGINT,
   order_product_id BIGINT,
   claim_type VARCHAR(50),
   claim_status VARCHAR(50) DEFAULT 'RECEIVED',
   extra_fee_payer VARCHAR(50),
   claim_reason VARCHAR(255),
   completed_at DATETIME,
   created_at DATETIME,
   updated_at DATETIME
);

DROP TABLE IF EXISTS claim_refund_history;
CREATE TABLE claim_refund_history (
  claim_refund_history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claim_id BIGINT,
  seller_id BIGINT,
  refund_cash_amount DECIMAL(19, 2) DEFAULT 0.00,
  promotion_discount_amount DECIMAL(19, 2) DEFAULT 0.00,
  coupon_discount_amount DECIMAL(19, 2) DEFAULT 0.00,
  refund_mileage_amount DECIMAL(19, 2) DEFAULT 0.00,
  subtract_delivery_amount DECIMAL(19, 2) DEFAULT 0.00,
  refund_delivery_amount DECIMAL(19, 2) DEFAULT 0.00,
  refund_at DATETIME,
  created_at DATETIME,
  updated_at DATETIME
);

-- 셀러별 일일 정산 테이블
DROP TABLE IF EXISTS daily_settlement;
CREATE TABLE daily_settlement (
  settlement_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT NOT NULL,
  settlement_date DATETIME NOT NULL,
  total_order_count INT NOT NULL DEFAULT 0,
  total_claim_count INT NOT NULL DEFAULT 0,
  total_product_count INT NOT NULL DEFAULT 0,
  total_quantity INT NOT NULL DEFAULT 0,
  tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  sales_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  promotion_discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  coupon_discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  point_used_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  shipping_fee DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  claim_shipping_fee DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  commission_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  total_settlement_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_seller_date (seller_id, settlement_date)
);

-- 상품별 정산 상세 테이블
DROP TABLE IF EXISTS daily_settlement_detail;
CREATE TABLE daily_settlement_detail (
 daily_settlement_detail_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 daily_settlement_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 order_product_id BIGINT,
 order_id BIGINT,
 quantity INT NOT NULL DEFAULT 0,
 settlement_status VARCHAR(20) NOT NULL,
 tax_type VARCHAR(20) NOT NULL,
 tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 sales_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 promotion_discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 coupon_discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 point_used_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 shipping_fee DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 claim_shipping_fee DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 commission_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 product_settlement_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
 created_at DATETIME,
 updated_at DATETIME,
 INDEX idx_settlement_id (daily_settlement_id),
 INDEX idx_product_id (product_id),
 INDEX idx_order_id (order_id),
 FOREIGN KEY (daily_settlement_id) REFERENCES daily_settlement(settlement_id) ON DELETE CASCADE
);

USE commerce;
--
-- -- 성능 최적화를 위한 설정 변경
SET GLOBAL innodb_flush_log_at_trx_commit = 0;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- 기존 인덱스 비활성화
ALTER TABLE seller DISABLE KEYS;

-- 1. seller 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/seller.csv'
INTO TABLE seller
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE seller ENABLE KEYS;

-- 기존 인덱스 비활성화
ALTER TABLE customer DISABLE KEYS;

-- 2. customer 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/customer.csv'
INTO TABLE customer
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE customer ENABLE KEYS;

-- 기존 인덱스 비활성화
ALTER TABLE products DISABLE KEYS;

-- 3. products 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/products.csv'
INTO TABLE products
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE products ENABLE KEYS;

-- 기존 인덱스 비활성화
ALTER TABLE orders DISABLE KEYS;

-- 4. orders 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/orders.csv'
INTO TABLE orders
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE orders ENABLE KEYS;

-- 기존 인덱스 비활성화
ALTER TABLE order_product DISABLE KEYS;

-- 5. order_product 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/orderProduct.csv'
INTO TABLE order_product
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(order_product_id, order_id, product_id, quantity, delivery_status,
@purchase_confirmed_at, @delivery_completed_at, created_at, updated_at)
SET
purchase_confirmed_at = NULLIF(@purchase_confirmed_at, 'null'),
delivery_completed_at = NULLIF(@delivery_completed_at, 'null');

-- CSV의 값을 임시 변수(@purchase_confirmed_at, @delivery_completed_at)로 읽습니다.
-- NULLIF() 함수를 사용하여 'null' 문자열이면 실제 NULL 값으로 변환합니다.
-- 이렇게 하면 CSV 파일의 'null' 문자열이 데이터베이스의 NULL 값으로 올바르게 로드됩니다.

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE order_product ENABLE KEYS;

-- 기존 인덱스 비활성화
ALTER TABLE claim DISABLE KEYS;

-- 6. claim 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/claim.csv'
INTO TABLE claim
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(claim_id, customer_id, order_product_id, claim_type, claim_status,
extra_fee_payer, claim_reason, @completed_at, created_at, updated_at)
SET
completed_at = NULLIF(@completed_at, 'null');

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE claim ENABLE KEYS;

-- 기존 인덱스 비활성화
ALTER TABLE claim_refund_history DISABLE KEYS;

-- 7. claim_refund_history 테이블 로드
LOAD DATA INFILE '/var/lib/mysql-files/claimRefundHistory.csv'
INTO TABLE claim_refund_history
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

-- 트랜잭션 커밋
COMMIT;

-- 인덱스 재활성화
ALTER TABLE claim_refund_history ENABLE KEYS;

-- 제약 조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- MySQL 설정 복원
SET GLOBAL innodb_flush_log_at_trx_commit = 1;
