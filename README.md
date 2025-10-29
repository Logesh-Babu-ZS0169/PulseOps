# Inbound Ingestion Metrics Dashboard

A standalone real-time monitoring dashboard for visualizing inbound document ingestion metrics with comprehensive failure recovery reports. Built with Java 11 Spring Boot backend and Next.js 13 frontend, fully containerized with Docker.

![Dashboard Features](https://img.shields.io/badge/Dashboard-Metrics%20%2B%20Recovery-purple)
![Java](https://img.shields.io/badge/Java-11-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)
![Next.js](https://img.shields.io/badge/Next.js-13.4.6-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

## 📊 Features

### Main Metrics Dashboard (Tab 0)
- **📅 Date Picker**: Select any specific date to view historical metrics data
- **Real-time Metrics Display**: Daily total inbound documents count for selected date
- **Hourly Analytics**: Separate tracking for MEDICAL_COMMERCIAL and MEDICAL_GBD document types
- **Interactive Visualizations**: 
  - Line charts for hourly trends
  - Bar charts for status breakdowns
  - Detailed data tables with Material-UI
- **Auto-refresh**: Dashboard automatically updates every hour for selected date
- **Advanced UI**: Professional purple gradient design with summary cards

### Failure Recovery Reports (Tab 1)
- **📅 Date Picker**: Select any specific date to view historical failure recovery data
- **📥 XLSX Download**: Download all 5 reports in Excel format with separate sheets
- **Tables Display**: Shows last 5 rows of each report for quick overview
- **Recovered from Inbound Failed**: Documents that initially failed but later succeeded
- **Kill Switch - Documents Waiting**: Documents exceeding waiting time threshold
- **Inbound Failed Status**: Files that failed during download phase
- **Process Failed Status**: Documents that failed during processing
- **Aborted Status**: Pipeline jobs aborted mid-way

### Additional Features
- **Email Notifications**: Automated hourly email alerts when new data rows are detected
  - Separate monitoring for MEDICAL_COMMERCIAL and MEDICAL_GBD
  - Detailed metrics breakdown in email
  - Configurable recipient (logesh.babu@intics.ai)
- **Tabbed Navigation**: Clean Material-UI tabs for switching between metrics and reports
- **Docker Ready**: Fully containerized with multi-stage builds for production deployment

## 🏗️ Architecture

### Backend (Java Spring Boot)
- **Technology**: Java 11, Spring Boot 2.7.18, HikariCP, Spring Mail
- **Port**: 8181
- **Database**: PostgreSQL with custom queries for metrics aggregation and failure recovery
- **API Endpoints**:
  - **Main Metrics (with optional date parameter):**
    - `GET /api/metrics/daily-total?date=2025-10-16` - Returns total inbound count for specified date
    - `GET /api/metrics/hourly/commercial?date=2025-10-16` - Returns hourly metrics for MEDICAL_COMMERCIAL
    - `GET /api/metrics/hourly/gbd?date=2025-10-16` - Returns hourly metrics for MEDICAL_GBD
  - **Failure Recovery (with optional date parameter):**
    - `GET /api/failure-recovery/recovered?date=2025-10-16` - Recovered from Inbound Failed documents
    - `GET /api/failure-recovery/waiting?date=2025-10-16` - Kill Switch - Documents waiting to process
    - `GET /api/failure-recovery/inbound-failed?date=2025-10-16` - Inbound failed status documents
    - `GET /api/failure-recovery/process-failed?date=2025-10-16` - Process failed status documents
    - `GET /api/failure-recovery/aborted?date=2025-10-16` - Aborted status documents
    - `GET /api/failure-recovery/download-excel?date=2025-10-16` - Download all reports as XLSX file
- **Scheduled Tasks**:
  - Hourly cron jobs (every hour at minute 0) to check for new data rows
  - Automated email notifications with detailed metrics breakdown

### Frontend (Next.js)
- **Technology**: Next.js 13.4.6, React 18, Material-UI (MUI) v5, Recharts, Dayjs
- **Port**: 5000
- **Features**: 
  - **Date Picker**: Material-UI DatePicker in both tabs to select and filter data by specific date
    - Separate date state for Main Metrics and Failure Recovery tabs
    - Auto-refresh on date change
  - Tabbed navigation with Material-UI Tabs (Main Metrics + Failure Recovery Reports)
  - Material-UI gradient cards with key metrics
  - Interactive charts (line and bar charts)
  - Detailed hourly data tables with MUI components
  - 5 comprehensive failure recovery report tables with color-coded headers
  - Automatic refresh every hour for selected date
  - Manual refresh button per tab with loading indicators

## 📁 Project Structure

```
.
├── k8s/                          # Kubernetes manifests
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── backend-deployment.yaml
│   ├── frontend-deployment.yaml
│   ├── ingress.yaml
│   └── kustomization.yaml
│
├── argocd/                       # ArgoCD configuration
│   └── application.yaml
│
├── helm/                         # Helm chart
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│
├── backend/                      # Java Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/intics/metrics/
│   │   │   │       ├── MetricsDashboardApplication.java
│   │   │   │       ├── config/
│   │   │   │       │   └── CorsConfig.java
│   │   │   │       ├── controller/
│   │   │   │       │   ├── MetricsController.java
│   │   │   │       │   └── FailureRecoveryController.java
│   │   │   │       ├── service/
│   │   │   │       │   ├── MetricsService.java
│   │   │   │       │   ├── FailureRecoveryService.java
│   │   │   │       │   ├── EmailService.java
│   │   │   │       │   └── ScheduledMetricsChecker.java
│   │   │   │       └── model/
│   │   │   │           ├── DailyTotalResponse.java
│   │   │   │           ├── HourlyMetrics.java
│   │   │   │           ├── RecoveredDocument.java
│   │   │   │           ├── WaitingDocument.java
│   │   │   │           ├── InboundFailedDocument.java
│   │   │   │           ├── ProcessFailedDocument.java
│   │   │   │           └── AbortedDocument.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   ├── Dockerfile                # Multi-stage Docker build
│   ├── .dockerignore
│   ├── pom.xml
│   └── EMAIL_SETUP.md            # Email configuration guide
│
├── frontend/                     # Next.js frontend
│   ├── src/
│   │   └── app/
│   │       ├── globals.css
│   │       ├── layout.tsx
│   │       └── page.tsx          # Main dashboard with tabs
│   ├── Dockerfile                # Multi-stage Docker build
│   ├── .dockerignore
│   ├── package.json
│   ├── tsconfig.json
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   └── next.config.js
│
├── docker-compose.yml            # Docker orchestration
├── Jenkinsfile                   # CI/CD pipeline
├── .env.example                  # Environment template
├── .dockerignore                 # Docker build optimization
├── DEPLOYMENT.md                 # Docker deployment guide
├── KUBERNETES_DEPLOYMENT.md      # K8s deployment guide
└── README.md                     # This file
```

## 🚀 Getting Started

### Prerequisites

**For Docker Deployment (Recommended):**
- Docker Engine 20.10+
- Docker Compose 2.0+
- PostgreSQL database access

**For Manual Deployment:**
- Java 11 or higher
- Maven 3.6+
- Node.js 18+
- PostgreSQL database access

## 🐳 Docker Deployment (Recommended)

### Quick Start

1. **Configure Environment**
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` with your credentials:
   ```env
   DB_PASSWORD=your_database_password
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   MAIL_TO=logesh.babu@intics.ai
   ```

2. **Build and Start Services**
   ```bash
   docker-compose up -d --build
   ```

3. **Access the Dashboard**
   - Frontend: http://localhost:5000
   - Backend API: http://localhost:8181/api/metrics/daily-total

4. **View Logs**
   ```bash
   docker-compose logs -f
   ```

5. **Stop Services**
   ```bash
   docker-compose down
   ```

For complete Docker deployment instructions, troubleshooting, and production configuration, see **[DEPLOYMENT.md](DEPLOYMENT.md)**.

## ☸️ Kubernetes Deployment (Production)

### Prerequisites
- Kubernetes cluster (v1.24+)
- ArgoCD installed
- Jenkins server with Docker support
- Docker registry access

### Deployment Options

**Option 1: Kubectl (Simple)**
```bash
kubectl apply -f k8s/
```

**Option 2: ArgoCD (GitOps)**
```bash
kubectl apply -f argocd/application.yaml
```

**Option 3: Helm (Flexible)**
```bash
helm install metrics-dashboard ./helm \
  --namespace metrics-dashboard \
  --create-namespace
```

### CI/CD Pipeline

The included Jenkins pipeline automates:
1. ✅ Build backend (Maven)
2. ✅ Build frontend (npm)
3. ✅ Run tests
4. ✅ Build Docker images
5. ✅ Push to registry
6. ✅ Update K8s manifests
7. ✅ Trigger ArgoCD sync
8. ✅ Verify deployment

For complete Kubernetes, ArgoCD, and Jenkins setup, see **[KUBERNETES_DEPLOYMENT.md](KUBERNETES_DEPLOYMENT.md)**.

## 🛠️ Manual Configuration

#### Backend Configuration

Update `backend/src/main/resources/application.properties` with your database and email credentials:

**Database Configuration:**
```properties
spring.datasource.url=jdbc:postgresql://YOUR_HOST:5432/YOUR_DATABASE
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

**Email Configuration (SMTP):**
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

notification.email.from=noreply@intics.ai
notification.email.to=logesh.babu@intics.ai
```

**📧 For detailed email setup instructions, see [EMAIL_SETUP.md](backend/EMAIL_SETUP.md)**

### Running the Application

#### Option 1: Using Current Environment
Both services are already configured and running:
- Backend: `http://localhost:8181`
- Frontend: `http://localhost:5000`

#### Option 2: Running Manually

**Start Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Start Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## 📊 Dashboard Metrics

### Daily Total Count
Shows the total number of inbound documents processed since midnight today.

### Hourly Metrics (by Document Type)

For both MEDICAL_COMMERCIAL and MEDICAL_GBD document types, the dashboard tracks:

- **Total Ingestion**: Total documents processed in each hour
- **Staged**: Documents waiting in staging
- **In Progress**: Documents currently being processed
- **Completed**: Successfully processed documents
- **Failed (Process)**: Documents that failed during processing
- **Failed (Ingestion)**: Documents that failed during ingestion
- **Aborted**: Documents that were aborted

## 🔄 Data Refresh

- **Automatic**: Dashboard refreshes every 60 minutes (3,600,000 ms)
- **Manual**: Click the "Refresh Now" button in the header at any time

## 📧 Email Notifications

The system automatically monitors for new hourly data and sends email alerts:

- **Frequency**: Every hour at minute 0 (e.g., 1:00, 2:00, 3:00)
- **Monitoring**: Separate checks for MEDICAL_COMMERCIAL and MEDICAL_GBD
- **Recipient**: logesh.babu@intics.ai (configurable)
- **Content**: 
  - Summary alert with hour range and new row count
  - Detailed metrics breakdown with all status counts
  - Hourly data for Staged, In Progress, Completed, Failed, and Aborted

**Setup Instructions**: See [backend/EMAIL_SETUP.md](backend/EMAIL_SETUP.md) for complete configuration guide

## 🎨 UI Components

### Summary Cards
Four colorful cards at the top displaying:
1. Total Inbound Documents
2. Commercial Processed
3. GBD Processed  
4. Total Completed

### Hourly Trend Charts
Line charts showing the progression of:
- Total ingestion
- Completed documents
- Failed documents

### Status Breakdown Charts
Stacked bar charts displaying all status categories by hour.

### Data Tables
Detailed tables with all metrics for each hour, including time frames and counts.

## 🔧 Integration into Existing Alchemy Application

This is a standalone application that can be integrated into your existing Alchemy application using either of these approaches:

### Option 1: Docker Deployment (Recommended)

Run as separate containerized services and integrate via iframe or direct link.

**Advantages:**
- Clean separation of concerns
- Independent deployment and scaling
- Easy rollback and updates
- No code conflicts with existing application

**Steps:**
1. Deploy using Docker Compose (see [DEPLOYMENT.md](DEPLOYMENT.md))
2. Access the dashboard at `http://your-server:5000`
3. Embed in Alchemy using iframe or link from navigation menu

### Option 2: Code Integration

Copy the code directly into your existing Alchemy codebase.

**Backend Integration:**

1. Copy the following packages to your existing project:
   - `com.intics.metrics.controller.MetricsController`
   - `com.intics.metrics.controller.FailureRecoveryController`
   - `com.intics.metrics.service.MetricsService`
   - `com.intics.metrics.service.FailureRecoveryService`
   - `com.intics.metrics.service.EmailService`
   - `com.intics.metrics.service.ScheduledMetricsChecker`
   - `com.intics.metrics.model.*` (all 7 model classes)

2. The `CorsConfig` may not be needed if your existing app already handles CORS

3. Add `@EnableScheduling` annotation to your main Spring Boot application class

4. Ensure your existing application has:
   - `spring-boot-starter-web`
   - `spring-boot-starter-data-jpa`
   - `spring-boot-starter-mail`
   - `postgresql` driver
   - `HikariCP`

5. Configure email settings in your `application.properties` (see [EMAIL_SETUP.md](backend/EMAIL_SETUP.md))

**Frontend Integration:**

1. Copy `frontend/src/app/page.tsx` to your Next.js application as a new route
2. Copy `frontend/src/app/layout.tsx` MUI configuration if not already present
3. Update API endpoints if your backend runs on a different port or path
4. Ensure you have the required dependencies installed:
   ```bash
   npm install @mui/material @mui/icons-material @emotion/react @emotion/styled recharts axios
   ```

## 📝 Database Schema

The application expects the following PostgreSQL schemas and tables:
- `inbound_config.ingestion_file_details`
- `inbound_config.ingestion_downloaded_file_details`
- `info.source_of_origin`
- `info.asset`
- `batching.sub_batching_process_payload_queue`
- `alchemy_response.pipeline_error_details`
- `product_outbound.product_response_details`

## 🛠️ Technology Stack

**Backend:**
- Java 11
- Spring Boot 2.7.18
- Spring Data JPA
- Spring Boot Mail (Email Notifications)
- HikariCP (Connection Pooling)
- PostgreSQL JDBC Driver
- Lombok
- Spring Scheduling (Cron Jobs)

**Frontend:**
- Next.js 13.4.6
- React 18
- TypeScript
- Material-UI (MUI) v5
  - @mui/material
  - @mui/icons-material
  - @mui/lab
  - @emotion/react & @emotion/styled
- Recharts (for visualizations)
- Axios (for HTTP requests)

## 📄 License

This is a standalone dashboard application created for Intics internal use.

## 👨‍💻 Author

Created for PulseOps application - Inbound Ingestion Monitoring
