const { Router } = require('express');
const router = Router();

/**
 * GET /api/alerts
 * Get all alerts (optionally filtered by vehicle or status)
 */
router.get('/', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicle_id, acknowledged } = req.query;
  
  try {
    let query = `SELECT a.*, v.name as vehicle_name 
                 FROM alerts a 
                 LEFT JOIN vehicles v ON a.vehicle_id = v.id 
                 WHERE 1=1`;
    const params = [];
    let paramIndex = 1;
    
    if (vehicle_id) {
      query += ` AND a.vehicle_id = $${paramIndex++}`;
      params.push(vehicle_id);
    }
    
    if (acknowledged !== undefined) {
      query += ` AND a.acknowledged = $${paramIndex++}`;
      params.push(acknowledged === 'true');
    }
    
    query += ` ORDER BY a.created_at DESC`;
    
    const result = await db.query(query, params);
    res.json(result.rows);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/alerts/:id
 * Get a single alert by ID
 */
router.get('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  
  try {
    const result = await db.query(
      `SELECT a.*, v.name as vehicle_name 
       FROM alerts a 
       LEFT JOIN vehicles v ON a.vehicle_id = v.id 
       WHERE a.id = $1`,
      [id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Alert not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/alerts
 * Create a new alert
 */
router.post('/', async (req, res, next) => {
  const db = req.app.locals.db;
  const { vehicle_id, type, message, severity } = req.body;
  
  if (!type || !message) {
    return res.status(400).json({ error: 'type and message are required' });
  }
  
  try {
    const result = await db.query(
      `INSERT INTO alerts (vehicle_id, type, message, severity)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [vehicle_id, type, message, severity || 'info']
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/alerts/:id/acknowledge
 * Acknowledge an alert
 */
router.put('/:id/acknowledge', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  
  try {
    const result = await db.query(
      `UPDATE alerts 
       SET acknowledged = true, acknowledged_at = NOW()
       WHERE id = $1
       RETURNING *`,
      [id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Alert not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * DELETE /api/alerts/:id
 * Delete an alert
 */
router.delete('/:id', async (req, res, next) => {
  const db = req.app.locals.db;
  const { id } = req.params;
  
  try {
    const result = await db.query('DELETE FROM alerts WHERE id = $1 RETURNING *', [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Alert not found' });
    }
    res.json({ message: 'Alert deleted successfully' });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
