export default function VehicleSelector({ vehicles, selected, onSelect }) {
  if (vehicles.length === 0) {
    return (
      <div className="text-gray-400 text-sm">
        No vehicles configured
      </div>
    )
  }

  return (
    <select
      value={selected || ''}
      onChange={(e) => onSelect(parseInt(e.target.value))}
      className="bg-gray-700 border border-gray-600 rounded-lg px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
    >
      {vehicles.map(vehicle => (
        <option key={vehicle.id} value={vehicle.id}>
          {vehicle.name} {vehicle.make && `- ${vehicle.make} ${vehicle.model}`}
        </option>
      ))}
    </select>
  )
}
