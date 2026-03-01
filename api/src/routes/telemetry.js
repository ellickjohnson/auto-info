const { Router } = require('express');
const router = Router();

/**
 * POST /api/telemetry
 * Receive telemetry data from mobile app
 */
router.post('/', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicle_id, rpm, speed, coolant_temp, throttle_pos, fuel_level, oil_temp, battery_voltage, latitude, longitude } = req.body;
  
  if (!vehicle_id) {
    return res.status(400).json({ error: 'vehicle_id is required' });
  }
  
  try {
    const result = await db.query(
      `INSERT INTO telemetry (vehicle_id, rpm, speed, coolant_temp, throttle_pos, fuel_level, oil_temp, battery_voltage, latitude, longitude)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [vehicle_id, rpm, speed, coolant_temp, throttle_pos, fuel_level, oil_temp, battery_voltage, latitude, longitude]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/telemetry/:vehicleId
 * Get historical telemetry data for a vehicle
 */
router.get('/:vehicleId', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicleId } = req.params;
  const { from, to, limit = 100 } = req.query;
  
  try {
    let query = `SELECT * FROM telemetry WHERE vehicle_id = $1`;
    const params = [vehicleId];
    let paramIndex = 2;
    
    if (from) {
      query += ` AND time >= $${paramIndex++}`;
      params.push(from);
    }
    if (to) {
      query += ` AND time <= $${paramIndex++}`;
      params.push(to);
    }
    
    query += ` ORDER BY time DESC LIMIT $${paramIndex}`;
    params.push(parseInt(limit));
    
    const result = await db.query(query, params);
    res.json(result.rows);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/telemetry/:vehicleId/latest
 * Get the latest telemetry data for a vehicle
 */
router.get('/:vehicleId/latest', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicleId } = req.params;
  
  try {
    const result = await db.query(
      `SELECT * FROM telemetry WHERE vehicle_id = $1 ORDER BY time DESC LIMIT 1`,
      [vehicleId]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'No telemetry data found for this vehicle' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
