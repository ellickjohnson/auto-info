const { Router } = require('express');
const router = Router();

/**
 * GET /api/vehicles
 * List all vehicles
 */
router.get('/', async (req, res, next) => {
  const db = req.app.locals.db;
  
  try {
    const result = await db.query(
      `SELECT v.*, 
        (SELECT json_build_object('time', time, 'rpm', rpm, 'speed', speed, 'coolant_temp', coolant_temp)
         FROM telemetry t WHERE t.vehicle_id = v.id ORDER BY time DESC LIMIT 1) as latest_telemetry
       FROM vehicles v
       ORDER BY v.created_at DESC`
    );
    res.json(result.rows);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/vehicles/:id
 * Get a single vehicle by ID
 */
router.get('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  
  try {
    const result = await db.query(
      `SELECT v.*, 
        (SELECT json_build_object('time', time, 'rpm', rpm, 'speed', speed, 'coolant_temp', coolant_temp)
         FROM telemetry t WHERE t.vehicle_id = v.id ORDER BY time DESC LIMIT 1) as latest_telemetry
       FROM vehicles v WHERE v.id = $1`,
      [id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Vehicle not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/vehicles
 * Add a new vehicle
 */
router.post('/', async (req, res, next) => {
  const db = req.app.locals.db;
  const { name, make, model, year, vin, license_plate, color } = req.body;
  
  if (!name) {
    return res.status(400).json({ error: 'Vehicle name is required' });
  }
  
  try {
    const result = await db.query(
      `INSERT INTO vehicles (name, make, model, year, vin, license_plate, color)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [name, make, model, year, vin, license_plate, color]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/vehicles/:id
 * Update a vehicle
 */
router.put('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  const { name, make, model, year, vin, license_plate, color } = req.body;
  
  try {
    const result = await db.query(
      `UPDATE vehicles 
       SET name = COALESCE($1, name),
           make = COALESCE($2, make),
           model = COALESCE($3, model),
           year = COALESCE($4, year),
           vin = COALESCE($5, vin),
           license_plate = COALESCE($6, license_plate),
           color = COALESCE($7, color),
           updated_at = NOW()
       WHERE id = $8
       RETURNING *`,
      [name, make, model, year, vin, license_plate, color, id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Vehicle not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * DELETE /api/vehicles/:id
 * Delete a vehicle
 */
router.delete('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  
  try {
    const result = await db.query('DELETE FROM vehicles WHERE id = $1 RETURNING *', [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Vehicle not found' });
    }
    res.json({ message: 'Vehicle deleted successfully' });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
