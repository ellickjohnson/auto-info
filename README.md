# Auto-Info

Vehicle telemetry system with real-time dashboard. Collects OBD-II data from your vehicles and displays it on a web dashboard.

## Architecture

- **API Backend**: Node.js + Express with JWT authentication
- **Database**: PostgreSQL + TimescaleDB for time-series telemetry data
- **Web Dashboard**: React + Vite + TailwindCSS

## Quick Start

```bash
# Clone the repo
git clone https://github.com/ellickjohnson/auto-info.git
cd auto-info

# Start all services
docker-compose up -d

# API will be available at http://localhost:3000
# Web dashboard at http://localhost:5173
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/telemetry | Receive telemetry data from app |
| GET | /api/telemetry/:vehicleId | Get historical telemetry data |
| POST | /api/vehicles | Add a new vehicle |
| GET | /api/vehicles | List all vehicles |
| GET | /api/dtcs | Get diagnostic trouble codes |
| POST | /api/alerts | Create a new alert |

## Development

```bash
# API development
cd api
npm install
npm run dev

# Web development
cd web
npm install
npm run dev
```

## Docker Images

- `ghcr.io/ellickjohnson/auto-info-api:latest`
- `ghcr.io/ellickjohnson/auto-info-web:latest`

## License

MIT
