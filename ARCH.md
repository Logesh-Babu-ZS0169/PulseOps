# Inbound Ingestion Metrics Dashboard 

## Overview
This project delivers a standalone monitoring dashboard for real-time visualization of inbound document ingestion metrics. It specifically tracks MEDICAL_COMMERCIAL and MEDICAL_GBD document types with hourly granularity. The dashboard provides daily and hourly metrics, failure recovery reports, and next-day traffic predictions. The project aims to provide critical insights into document ingestion processes, enhance operational monitoring, and support proactive decision-making. It is designed for potential integration into an existing Alchemy Java application.

## User Preferences
- Uses Next.js version 13.4.6 specifically
- Prefers standalone application that can be integrated later into existing Alchemy Java application
- Uses PostgreSQL database at 192.168.10.248:5432/inticsdev
- Requires hourly automatic data refresh

## System Architecture
The system comprises a Java 11 Spring Boot backend and a Next.js 13.4.6 frontend. It connects to an external PostgreSQL database.

**UI/UX Decisions:**
- Modern dashboard UI built with Material-UI (MUI) v5, featuring gradient cards and professional design.
- Interactive visualizations (line charts, bar charts) using the Recharts library.
- Tabbed navigation separates "Main Metrics" from "Failure Recovery Reports."
- Date and Time pickers allow filtering of historical data.
- Color-coded sections and icons enhance visual clarity for failure reports.

**Technical Implementations & Feature Specifications:**
- **Backend (Java Spring Boot)**:
    - Provides REST APIs for daily/hourly metrics, failure recovery reports, and next-day predictions.
    - Implements `MetricsService`, `FailureRecoveryService`, and `PredictionService` for data aggregation and analysis.
    - Features email notifications for hourly data alerts via `EmailService` and scheduled cron jobs.
    - Utilizes Apache POI for XLSX export functionality for failure recovery reports.
    - Configured with HikariCP for database connection pooling and CORS for cross-origin requests.
- **Frontend (Next.js)**:
    - Single-page application with a main dashboard (`src/app/page.tsx`).
    - Displays daily totals, hourly breakdowns by document type, and status tracking (Staged, In Progress, Completed, Failed, Aborted).
    - **Next-Day Traffic Prediction**: Statistical analysis-based prediction (weighted combination of 7-day avg, day-of-week avg, 14-day avg), trend detection, and confidence scoring, presented in a dedicated card with historical trends.
    - **Failure Recovery Reports**: Displays five types of reports: Recovered from Inbound Failed, Kill Switch - Documents Waiting, Inbound Failed Status, Process Failed Status (with root_pipeline_id, action_id, and log columns from audit tables), and Aborted Status. Uses DISTINCT ON to prevent duplicate rows. Includes XLSX download.
    - Auto-refreshes data hourly.
- **System Design Choices:**
    - Modular architecture with clear separation of concerns (backend/frontend).
    - Containerized using Docker for both backend and frontend, orchestrated with `docker-compose.yml`.
    - Production-ready deployment setup including Kubernetes manifests, ArgoCD GitOps, and Jenkins CI/CD pipelines for automated deployments and resource management.

## External Dependencies
- **Database**: PostgreSQL (external at 192.168.10.248:5432)
- **Backend Libraries**:
    - Spring Boot 2.7.18
    - Spring Data JPA
    - Spring Boot Mail (for email notifications)
    - Apache POI 5.2.3 (for XLSX generation)
    - HikariCP
    - Lombok
- **Frontend Libraries**:
    - Next.js 13.4.6
    - React 18.2.0
    - Material-UI v5 (core, icons, lab, date-pickers)
    - Recharts 2.12.0
    - Axios 1.6.0
    - Dayjs 1.11.x (for date handling)
    - TypeScript 5.1.3