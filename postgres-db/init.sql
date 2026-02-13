-- =============================================================================
-- 1. TABLA DE LÍMITES (Perfilado de Cliente para Fraude)
-- =============================================================================
CREATE TABLE IF NOT EXISTS customer_limits (
    customer_id VARCHAR(36) PRIMARY KEY,
    daily_max_amount DECIMAL(18, 4) NOT NULL,
    current_daily_spent DECIMAL(18, 4) DEFAULT 0,
    last_reset TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Data de prueba: Diferentes escenarios de límites
INSERT INTO customer_limits (customer_id, daily_max_amount, current_daily_spent) VALUES
('CUST-001', 5000.00, 1200.00),
('CUST-002', 10000.00, 9500.00), -- Escenario: Al borde del límite
('CUST-003', 2000.00, 0.00),     -- Escenario: Nuevo/Sin gasto
('CUST-004', 500.00, 450.00),    -- Escenario: Límite muy bajo
('CUST-005', 15000.00, 2000.00),
('CUST-006', 3000.00, 2900.00),
('CUST-007', 8000.00, 100.00),
('CUST-008', 1200.00, 0.00),
('CUST-009', 25000.00, 5000.00),
('CUST-010', 100.00, 90.00);


-- =============================================================================
-- 2. TABLA DE TRANSACCIONES (Core Ledger)
-- =============================================================================
CREATE TABLE IF NOT EXISTS transactions (
    -- Identificadores únicos
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- UUID es mejor para sistemas distribuidos
    transaction_id VARCHAR(64) UNIQUE NOT NULL,    -- ID de negocio (ej: del switch transaccional)
    correlation_id VARCHAR(128),                    -- Para trazabilidad en microservicios
    
    -- Información de la Cuenta y Cliente
    account_id VARCHAR(36) NOT NULL,               -- ID interno de la cuenta
    customer_id VARCHAR(36) NOT NULL,              -- ID del cliente (para el perfilado de fraude)
    
    -- Detalles financieros
    amount DECIMAL(18, 4) NOT NULL,                -- 4 decimales para precisión bancaria
    currency CHAR(3) NOT NULL DEFAULT 'PEN',       -- ISO 4217 (Soles por defecto)
    operation_type VARCHAR(30) NOT NULL,           -- DEBIT, CREDIT, TRANSFER, CASH_OUT
    
    -- Información del Comercio / Origen
    merchant_id VARCHAR(50),
    merchant_name VARCHAR(150),
    mcc VARCHAR(4),                                -- Merchant Category Code (crítico para fraude)
    
    -- Metadatos de Red y Dispositivo
    terminal_id VARCHAR(20),
    ip_address VARCHAR(45),
    channel VARCHAR(20) NOT NULL,                  -- MOBILE_APP, WEB, POS, ATM
    
    -- Estado y Auditoría
    status VARCHAR(20) NOT NULL,                   -- PENDING, COMPLETED, FAILED, REVERSED
    response_code VARCHAR(5),                      -- Códigos tipo ISO-8583 (00: Exitoso, 51: Fondos insuficientes)
    description TEXT,
    
    -- Tiempos (Auditoría)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Data de prueba: Mix de canales y estados
INSERT INTO transactions (transaction_id, correlation_id, account_id, customer_id, amount, operation_type, merchant_name, mcc, channel, status, response_code) VALUES
('TXN-2024-001', 'CORR-001', 'ACC-101', 'CUST-001', 500.00, 'DEBIT', 'Saga Falabella', '5311', 'POS', 'COMPLETED', '00'),
('TXN-2024-002', 'CORR-002', 'ACC-102', 'CUST-002', 1200.00, 'TRANSFER', 'Interbank App', '4829', 'MOBILE_APP', 'COMPLETED', '00'),
('TXN-2024-003', 'CORR-003', 'ACC-101', 'CUST-001', 3500.00, 'DEBIT', 'IKEA Online', '5712', 'WEB', 'PENDING', '00'),
('TXN-2024-004', 'CORR-004', 'ACC-104', 'CUST-004', 100.00, 'CASH_OUT', 'Global Net ATM', '6011', 'ATM', 'FAILED', '51'),
('TXN-2024-005', 'CORR-005', 'ACC-105', 'CUST-005', 2000.00, 'DEBIT', 'Travel Agency', '4722', 'WEB', 'COMPLETED', '00'),
('TXN-2024-006', 'CORR-006', 'ACC-106', 'CUST-006', 150.00, 'DEBIT', 'Starbucks', '5812', 'POS', 'COMPLETED', '00'),
('TXN-2024-007', 'CORR-007', 'ACC-107', 'CUST-007', 8000.00, 'TRANSFER', 'BCP App', '4829', 'MOBILE_APP', 'COMPLETED', '00'),
('TXN-2024-008', 'CORR-008', 'ACC-108', 'CUST-008', 45.50, 'DEBIT', 'Uber', '4121', 'MOBILE_APP', 'COMPLETED', '00'),
('TXN-2024-009', 'CORR-009', 'ACC-109', 'CUST-009', 15000.00, 'DEBIT', 'Rolex Store', '5944', 'POS', 'PENDING', '00'),
('TXN-2024-010', 'CORR-010', 'ACC-110', 'CUST-010', 25.00, 'DEBIT', 'Oxxo', '5411', 'POS', 'COMPLETED', '00');

-- Índices para optimizar el motor de fraude (Flux asíncronos)
CREATE INDEX idx_transaction_customer ON transactions(customer_id);
CREATE INDEX idx_transaction_created_at ON transactions(created_at);


CREATE TABLE IF NOT EXISTS outbox_events (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'FAILED', -- FAILED, RETRYING, PROCESSED
    error_message TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);