import Gauge from './Gauge'

export default function GaugeCluster({ telemetry }) {
  if (!telemetry) {
    return (
      <div className="bg-gray-800 rounded-xl p-6 text-center">
        <p className="text-gray-400">Waiting for telemetry data...</p>
        <p className="text-gray-500 text-sm mt-2">Connect your vehicle to start receiving data</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      {/* RPM Gauge */}
      <div className="bg-gray-800 rounded-xl p-6">
        <Gauge 
          value={telemetry.rpm || 0} 
          max={8000} 
          label="RPM" 
          unit="rpm"
          color="#ef4444"
          warningThreshold={6000}
        />
      </div>

      {/* Speed Gauge */}
      <div className="bg-gray-800 rounded-xl p-6">
        <Gauge 
          value={telemetry.speed || 0} 
          max={240} 
          label="Speed" 
          unit="km/h"
          color="#3b82f6"
          warningThreshold={180}
        />
      </div>

      {/* Temperature Gauge */}
      <div className="bg-gray-800 rounded-xl p-6">
        <Gauge 
          value={telemetry.coolant_temp || 0} 
          max={130} 
          label="Coolant Temp" 
          unit="°C"
          color="#f59e0b"
          warningThreshold={100}
        />
      </div>

      {/* Additional readings */}
      <div className="bg-gray-800 rounded-xl p-6 md:col-span-3">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
          <div>
            <p className="text-gray-400 text-sm">Fuel Level</p>
            <p className="text-2xl font-bold text-green-400">
              {telemetry.fuel_level ? `${telemetry.fuel_level.toFixed(0)}%` : '--'}
            </p>
          </div>
          <div>
            <p className="text-gray-400 text-sm">Throttle</p>
            <p className="text-2xl font-bold text-yellow-400">
              {telemetry.throttle_pos ? `${telemetry.throttle_pos.toFixed(0)}%` : '--'}
            </p>
          </div>
          <div>
            <p className="text-gray-400 text-sm">Oil Temp</p>
            <p className="text-2xl font-bold text-orange-400">
              {telemetry.oil_temp ? `${telemetry.oil_temp}°C` : '--'}
            </p>
          </div>
          <div>
            <p className="text-gray-400 text-sm">Battery</p>
            <p className="text-2xl font-bold text-blue-400">
              {telemetry.battery_voltage ? `${telemetry.battery_voltage}V` : '--'}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
