const express = require('express');
const router = express.Router();

/**
 * @route POST /api/v1/magiclink/send
 * @desc Receives magic link payload from Keycloak plugin and echoes it back
 */
router.post('/send', (req, res) => {
  // Log the received payload
  console.log('[MOCK-API] Magic link payload:', req.body);

  // Respond with success and echo the received data
  return res.json({
    success: true,
    message: 'Magic link received and processed',
    received: req.body
  });
});

module.exports = router; 