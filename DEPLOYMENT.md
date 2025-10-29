# Docker Deployment Guide

This guide explains how to deploy the Inbound Ingestion Metrics Dashboard using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Access to PostgreSQL database at 192.168.10.248:5432
- SMTP credentials for email notifications

## Quick Start

### 1. Configure Environment Variables

Copy the example environment file and update with your credentials:

```bash
cp .env.example .env
```

Edit `.env` and set the following:

```env
# Database Configuration
DB_PASSWORD=your_database_password

# Email Configuration (for notifications)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_TO=logesh.babu@intics.ai
```

### 2. Build and Start Services

Build and start all services using Docker Compose:

```bash
docker-compose up -d --build
```

This will:
- Build the backend Spring Boot application
- Build the frontend Next.js application
- Start both services in the background
- Create a network for inter-service communication

### 3. Verify Deployment

Check that both services are running:

```bash
docker-compose ps
```

You should see both `metrics-dashboard-backend` and `metrics-dashboard-frontend` running.

### 4. Access the Dashboard

- **Frontend**: http://localhost:5000
- **Backend API**: http://localhost:8181/api/metrics/daily-total

## Service Details

### Backend Service
- **Port**: 8181
- **Health Check**: http://localhost:8181/actuator/health
- **API Endpoints**:
  - Main Metrics: `/api/metrics/*`
  - Failure Recovery: `/api/failure-recovery/*`

### Frontend Service
- **Port**: 5000
- **Features**:
  - Main Metrics Dashboard (Tab 0)
  - Failure Recovery Reports (Tab 1)
  - Auto-refresh every 60 minutes

## Management Commands

### View Logs

View logs for all services:
```bash
docker-compose logs -f
```

View logs for specific service:
```bash
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Stop Services

```bash
docker-compose down
```

### Restart Services

```bash
docker-compose restart
```

### Rebuild After Code Changes

```bash
docker-compose up -d --build
```

## Production Configuration

### Email Scheduler

The backend includes scheduled jobs that check for new metrics hourly and send email notifications. Currently set to run every 5 minutes for testing.

To change to production hourly schedule, update in `backend/src/main/java/com/intics/metrics/service/ScheduledMetricsChecker.java`:

```java
// Change from testing (every 5 minutes)
@Scheduled(cron = "0 */5 * * * *")

// To production (every hour at minute 0)
@Scheduled(cron = "0 0 * * * *")
```

### Database Connection

The application connects to PostgreSQL at `192.168.10.248:5432/inticsdev`. Ensure:
- Database is accessible from your Docker host
- Firewall rules allow connections
- Credentials are correct in `.env`

### Email Configuration

For Gmail, you need to:
1. Enable 2-factor authentication
2. Generate an App Password
3. Use the App Password in `MAIL_PASSWORD`

For other SMTP providers, update `MAIL_HOST` and `MAIL_PORT` accordingly.

## Integration with Alchemy Application

To integrate this dashboard into your existing Alchemy application:

### Option 1: Run as Separate Services
Keep the dashboard running in Docker and access it via iframe or direct link from Alchemy.

### Option 2: Import Code Directly
1. Copy backend controllers, services, and models to Alchemy's Java codebase
2. Copy frontend page to Alchemy's Next.js application
3. Adjust API endpoint paths if needed
4. Ensure all dependencies are included

## Troubleshooting

### Backend Can't Connect to Database
- Verify database host is accessible: `ping 192.168.10.248`
- Check firewall rules
- Verify credentials in `.env`

### Frontend Can't Connect to Backend
- Ensure backend is running: `docker-compose ps`
- Check backend logs: `docker-compose logs backend`
- Verify network configuration in `docker-compose.yml`

### Email Notifications Not Sending
- Check SMTP credentials in `.env`
- View backend logs for error messages
- Verify SMTP server allows connections from Docker host

## Health Checks

Both services include health checks:
- Backend: Checks Spring Boot actuator endpoint
- Frontend: Checks Next.js server availability

View health status:
```bash
docker-compose ps
```

Unhealthy services will show `(unhealthy)` in the status.

## Resource Requirements

Recommended resources:
- **CPU**: 2 cores minimum
- **Memory**: 2GB minimum (512MB backend, 512MB frontend)
- **Disk**: 1GB for images

Adjust memory limits in `docker-compose.yml` if needed:

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 1G
```

## Network Configuration

The services use a custom bridge network (`metrics-network`) for isolation and security. The frontend communicates with the backend using the service name `backend:8181`.

For external access in production:
- Use a reverse proxy (nginx, Apache)
- Configure SSL/TLS certificates
- Set up proper domain names

