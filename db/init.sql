-- Auto-Info Database Schema
-- PostgreSQL + TimescaleDB

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    make VARCHAR(50),
    model VARCHAR(50),
    year INTEGER,
    vin VARCHAR(17) UNIQUE,
    license_plate VARCHAR(20),
    color VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Telemetry table (will be converted to hypertable)
CREATE TABLE IF NOT EXISTS telemetry (
    time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    rpm INTEGER,
    speed INTEGER, -- km/h
    coolant_temp INTEGER, -- Celsius
    throttle_pos DECIMAL(5,2), -- percentage 0-100
    fuel_level DECIMAL(5,2), -- percentage 0-100
    oil_temp INTEGER, -- Celsius
    battery_voltage DECIMAL(4,2), -- volts
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8)
);

-- Convert telemetry to hypertable (time-series)
SELECT create_hypertable('telemetry', 'time', if_not_exists => TRUE);

-- Create index for faster vehicle queries
CREATE INDEX IF NOT EXISTS idx_telemetry_vehicle_id ON telemetry (vehicle_id, time DESC);

-- DTCs (Diagnostic Trouble Codes) table
CREATE TABLE IF NOT EXISTS dtcs (
    id SERIAL PRIMARY KEY,
    vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    description TEXT,
    severity VARCHAR(20) DEFAULT 'medium' CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    active BOOLEAN DEFAULT TRUE,
    detected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Alerts table
CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    vehicle_id INTEGER REFERENCES vehicles(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) DEFAULT 'info' CHECK (severity IN ('info', 'warning', 'error', 'critical')),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_dtcs_vehicle_id ON dtcs (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_dtcs_active ON dtcs (active);
CREATE INDEX IF NOT EXISTS idx_alerts_vehicle_id ON alerts (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_alerts_acknowledged ON alerts (acknowledged);

-- Continuous aggregate for hourly telemetry stats (optional, useful for dashboards)
CREATE MATERIALIZED VIEW IF NOT EXISTS telemetry_hourly
WITH (timescaledb.continuous) AS
SELECT 
    vehicle_id,
    time_bucket('1 hour', time) AS bucket,
    AVG(rpm) AS avg_rpm,
    MAX(rpm) AS max_rpm,
    AVG(speed) AS avg_speed,
    MAX(speed) AS max_speed,
    AVG(coolant_temp) AS avg_coolant_temp,
    MAX(coolant_temp) AS max_coolant_temp
FROM telemetry
GROUP BY vehicle_id, time_bucket('1 hour', time)
WITH DATA;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('telemetry_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for vehicles table
CREATE TRIGGER update_vehicles_updated_at
    BEFORE UPDATE ON vehicles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample vehicle for testing
INSERT INTO vehicles (name, make, model, year, color) 
VALUES ('Daily Driver', 'Toyota', 'Camry', 2020, 'Silver')
ON CONFLICT DO NOTHING;
