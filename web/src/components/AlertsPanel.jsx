export default function AlertsPanel({ alerts, onAcknowledge }) {
  const severityColors = {
    info: 'bg-blue-500/20 border-blue-500 text-blue-400',
    warning: 'bg-yellow-500/20 border-yellow-500 text-yellow-400',
    error: 'bg-red-500/20 border-red-500 text-red-400',
    critical: 'bg-red-700/30 border-red-600 text-red-300'
  }

  if (alerts.length === 0) {
    return (
      <div className="bg-gray-800 rounded-xl p-4">
        <h2 className="text-lg font-semibold text-gray-300 mb-2">Recent Alerts</h2>
        <p className="text-gray-500 text-sm">No active alerts 🎉</p>
      </div>
    )
  }

  return (
    <div className="bg-gray-800 rounded-xl p-4">
      <h2 className="text-lg font-semibold text-gray-300 mb-4">Recent Alerts</h2>
      <div className="space-y-2">
        {alerts.map(alert => (
          <div 
            key={alert.id}
            className={`border rounded-lg p-3 flex items-center justify-between ${severityColors[alert.severity] || severityColors.info}`}
          >
            <div>
              <p className="font-medium">{alert.message}</p>
              <p className="text-xs opacity-75">
                {alert.vehicle_name && `${alert.vehicle_name} • `}
                {new Date(alert.created_at).toLocaleString()}
              </p>
            </div>
            <button
              onClick={() => onAcknowledge(alert.id)}
              className="px-3 py-1 bg-white/10 hover:bg-white/20 rounded text-sm transition-colors"
            >
              Acknowledge
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
