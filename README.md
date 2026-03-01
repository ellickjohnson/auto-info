# Auto-Info

Vehicle telemetry system with real-time dashboard. Collects OBD-II data from your vehicles and displays it on a web dashboard.

## Architecture

- **API Backend**: Node.js + Express with JWT authentication
- **Database**: PostgreSQL + TimescaleDB for time-series telemetry data
- **Web Dashboard**: React + Vite + TailwindCSS
- **Android App**: Kotlin + Jetpack Compose for OBD-II data collection

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

## Android App

The Auto-Info Android app connects to ELM327 Bluetooth OBD-II adapters to collect real-time vehicle telemetry.

### Features

- **Bluetooth OBD-II Connection**: Connects to any ELM327-compatible adapter
- **Real-time PIDs**: Reads RPM, Speed, Coolant Temp, Throttle Position, Fuel Level, Engine Load
- **Auto-upload**: Sends data to your Auto-Info API automatically
- **Offline Cache**: Stores data locally when offline, syncs when connected
- **Vehicle Selection**: Enter vehicle ID or scan QR code
- **Configurable Settings**: API URL, polling interval, vehicle ID

### Installation

1. Download the latest APK from [GitHub Releases](https://github.com/ellickjohnson/auto-info/releases)
2. Enable "Install from Unknown Sources" on your Android device
3. Install the APK
4. Pair your ELM327 adapter in Bluetooth settings first
5. Open the app, enter your vehicle ID, and start collecting!

### Building from Source

```bash
cd android
./gradlew assembleDebug
# APK will be in app/build/outputs/apk/debug/
```

### Requirements

- Android 8.0+ (API 26+)
- Bluetooth capability
- ELM327 OBD-II Bluetooth adapter (any compatible model)
- Location permission (required for Bluetooth LE on Android 12+)

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

# Android development
cd android
./gradlew installDebug
```

## Docker Images

- `ghcr.io/ellickjohnson/auto-info-api:latest`
- `ghcr.io/ellickjohnson/auto-info-web:latest`

## OBD-II PIDs Supported

| PID | Code | Description |
|-----|------|-------------|
| 0x0C | 010C | Engine RPM |
| 0x0D | 010D | Vehicle Speed |
| 0x05 | 0105 | Engine Coolant Temperature |
| 0x11 | 0111 | Throttle Position |
| 0x2F | 012F | Fuel Tank Level |
| 0x04 | 0104 | Calculated Engine Load |

## License

MIT
