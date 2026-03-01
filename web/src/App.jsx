import { useState, useEffect } from 'react'
import VehicleSelector from './components/VehicleSelector'
import GaugeCluster from './components/GaugeCluster'
import AlertsPanel from './components/AlertsPanel'

const API_URL = '/api'

function App() {
  const [vehicles, setVehicles] = useState([])
  const [selectedVehicle, setSelectedVehicle] = useState(null)
  const [telemetry, setTelemetry] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Fetch vehicles on mount
  useEffect(() => {
    fetchVehicles()
  }, [])

  // Fetch telemetry when vehicle is selected
  useEffect(() => {
    if (selectedVehicle) {
      fetchTelemetry()
      const interval = setInterval(fetchTelemetry, 5000) // Poll every 5 seconds
      return () => clearInterval(interval)
    }
  }, [selectedVehicle])

  // Fetch alerts
  useEffect(() => {
    fetchAlerts()
    const interval = setInterval(fetchAlerts, 30000) // Poll every 30 seconds
    return () => clearInterval(interval)
  }, [])

  const fetchVehicles = async () => {
    try {
      const res = await fetch(`${API_URL}/vehicles`)
      if (!res.ok) throw new Error('Failed to fetch vehicles')
      const data = await res.json()
      setVehicles(data)
      if (data.length > 0 && !selectedVehicle) {
        setSelectedVehicle(data[0].id)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const fetchTelemetry = async () => {
    if (!selectedVehicle) return
    try {
      const res = await fetch(`${API_URL}/telemetry/${selectedVehicle}/latest`)
      if (res.ok) {
        const data = await res.json()
        setTelemetry(data)
      }
    } catch (err) {
      console.error('Failed to fetch telemetry:', err)
    }
  }

  const fetchAlerts = async () => {
    try {
      const res = await fetch(`${API_URL}/alerts?acknowledged=false`)
      if (res.ok) {
        const data = await res.json()
        setAlerts(data.slice(0, 10)) // Show last 10 unacknowledged
      }
    } catch (err) {
      console.error('Failed to fetch alerts:', err)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-red-500 text-xl">Error: {error}</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-900">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary-400">
            🚗 Auto-Info
          </h1>
          <VehicleSelector 
            vehicles={vehicles} 
            selected={selectedVehicle} 
            onSelect={setSelectedVehicle} 
          />
        </div>
      </header>

      <main className="p-6">
        {/* Gauges */}
        <div className="mb-8">
          <GaugeCluster telemetry={telemetry} />
        </div>

        {/* Alerts Panel */}
        <AlertsPanel 
          alerts={alerts} 
          onAcknowledge={async (alertId) => {
            try {
              await fetch(`${API_URL}/alerts/${alertId}/acknowledge`, { method: 'PUT' })
              fetchAlerts()
            } catch (err) {
              console.error('Failed to acknowledge alert:', err)
            }
          }}
        />

        {/* Vehicle info */}
        {selectedVehicle && (
          <div className="mt-6 bg-gray-800 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-gray-300 mb-2">Vehicle Info</h2>
            {vehicles.find(v => v.id === selectedVehicle) && (
              <div className="text-gray-400">
                <p>{vehicles.find(v => v.id === selectedVehicle).name}</p>
                <p className="text-sm">
                  {vehicles.find(v => v.id === selectedVehicle).make} {' '}
                  {vehicles.find(v => v.id === selectedVehicle).model} {' '}
                  {vehicles.find(v => v.id === selectedVehicle).year}
                </p>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}

export default App
