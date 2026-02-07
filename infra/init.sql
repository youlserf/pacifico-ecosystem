-- Database for Quotation Microservice
CREATE DATABASE quotation_db;
-- Database for Issuance Microservice
CREATE DATABASE issuance_db;

\c quotation_db;

CREATE TABLE quotes (
    id SERIAL PRIMARY KEY,
    dni VARCHAR(20) NOT NULL,
    age INT NOT NULL,
    car_value DECIMAL(12, 2) NOT NULL,
    probability_score DOUBLE PRECISION,
    risk_level VARCHAR(20),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

\c issuance_db;

CREATE TABLE policies (
    id SERIAL PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    policy_number VARCHAR(50) UNIQUE NOT NULL,
    dni VARCHAR(20) NOT NULL,
    final_premium DECIMAL(12, 2) NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
