# French Energy Certificate Auction System

A complete web application that automatically tracks and displays French energy certificate auction data.

## Features

- **Automated Data Collection**: Daily scraping of auction results
- **Interactive Dashboard**: Real-time data visualization with charts
- **Regional Analysis**: Breakdown across all French regions
- **Technology Insights**: Wind, Solar, Hydro, and Thermal data
- **Historical Tracking**: Complete auction history and trends

## Tech Stack

**Backend:**
- Java 21 + Spring Boot 3.5
- PostgreSQL 17
- JSoup for web scraping
- Maven

**Frontend:**
- React 19 + TypeScript
- SCSS styling
- Recharts for visualization
- Docker + Nginx

## Getting Started

### Prerequisites
- Java 21
- Node.js 18+
- Docker (for database)

### Backend Setup
```bash
# Clone repository
git clone <repository-url>
cd backend

# Start database
docker-compose up -d

# Set environment variables
export DB_USERNAME=auction_user
export DB_PASSWORD=auction_pass
export DB_URL=jdbc:postgresql://localhost:5435/french_energy_auctions

# Run application
./mvnw spring-boot:run
```

### Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

Visit `http://localhost:3000` to view the dashboard.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auctions` | All auction data |
| GET | `/api/auctions/latest` | Most recent auction |
| GET | `/api/auctions/stats` | Summary statistics |
| GET | `/api/auctions/health` | System health check |

## Environment Variables

**Backend:**
```env
DB_USERNAME=auction_user
DB_PASSWORD=auction_pass
DB_URL=jdbc:postgresql://localhost:5435/french_energy_auctions
```

**Frontend:**
```env
REACT_APP_API_URL=http://localhost:8080
```

## Docker Deployment

```bash
# Backend
docker build -t french-auction-backend .
docker run -p 8080:8080 french-auction-backend

# Frontend
docker build -t french-auction-frontend .
docker run -p 3000:80 french-auction-frontend
```