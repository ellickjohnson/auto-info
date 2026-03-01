const { Router } = require('express');
const router = Router();

/**
 * GET /api/dtcs
 * Get all diagnostic trouble codes (optionally filtered by vehicle)
 */
router.get('/', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicle_id, active } = req.query;
  
  try {
    let query = `SELECT d.*, v.name as vehicle_name 
                 FROM dtcs d 
                 LEFT JOIN vehicles v ON d.vehicle_id = v.id 
                 WHERE 1=1`;
    const params = [];
    let paramIndex = 1;
    
    if (vehicle_id) {
      query += ` AND d.vehicle_id = $${paramIndex++}`;
      params.push(vehicle_id);
    }
    
    if (active !== undefined) {
      query += ` AND d.active = $${paramIndex++}`;
      params.push(active === 'true');
    }
    
    query += ` ORDER BY d.detected_at DESC`;
    
    const result = await db.query(query, params);
    res.json(result.rows);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/dtcs/:id
 * Get a single DTC by ID
 */
router.get('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  
  try {
    const result = await db.query(
      `SELECT d.*, v.name as vehicle_name 
       FROM dtcs d 
       LEFT JOIN vehicles v ON d.vehicle_id = v.id 
       WHERE d.id = $1`,
      [id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'DTC not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/dtcs
 * Create a new DTC (usually from the mobile app)
 */
router.post('/', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicle_id, code, description, severity } = req.body;
  
  if (!vehicle_id || !code) {
    return res.status(400).json({ error: 'vehicle_id and code are required' });
  }
  
  try {
    const result = await db.query(
      `INSERT INTO dtcs (vehicle_id, code, description, severity)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [vehicle_id, code, description, severity || 'medium']
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/dtcs/:id
 * Update a DTC (e.g., mark as resolved)
 */
router.put('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  const { active, resolved_at } = req.body;
  
  try {
    const result = await db.query(
      `UPDATE dtcs 
       SET active = COALESCE($1, active),
           resolved_at = COALESCE($2, resolved_at)
       WHERE id = $3
       RETURNING *`,
      [active, resolved_at, id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'DTC not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
