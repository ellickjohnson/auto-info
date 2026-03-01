require('dotenv').config();
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const { Pool } = require('pg');

// Routes
const telemetryRoutes = require('./routes/telemetry');
const vehicleRoutes = require('./routes/vehicles');
const dtcRoutes = require('./routes/dtcs');
const alertRoutes = require('./routes/alerts');
const authMiddleware = require('./middleware/auth');

const app = express();

// Database connection
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

// Make pool available to routes
app.locals.db = pool;

// Middleware
app.use(cors());
app.use(express.json());

// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100,
  message: { error: 'Too many requests, please try again later.' }
});
app.use('/api/', limiter);

// Health check (no auth required)
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Public routes (no auth for MVP - add auth in production)
app.use('/api/telemetry', telemetryRoutes);
app.use('/api/vehicles', vehicleRoutes);
app.use('/api/dtcs', dtcRoutes);
app.use('/api/alerts', alertRoutes);

// Error handler
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    error: err.message || 'Internal server error'
  });
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Auto-Info API running on port ${PORT}`);
});

module.exports = app;
