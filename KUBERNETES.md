# Kubernetes Deployment Guide
## Metrics Dashboard - Production Kubernetes Deployment

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Kubernetes Resources](#kubernetes-resources)
4. [Deployment Methods](#deployment-methods)
5. [Configuration Management](#configuration-management)
6. [Monitoring & Logging](#monitoring--logging)
7. [Scaling & Performance](#scaling--performance)
8. [Security Best Practices](#security-best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Application Components
```
┌─────────────────────────────────────────────────────┐
│                   Ingress Controller                 │
│              (NGINX/Traefik/ALB)                    │
└───────────┬─────────────────────────────┬───────────┘
            │                             │
    ┌───────▼────────┐          ┌────────▼──────────┐
    │   Frontend     │          │    Backend        │
    │  (Next.js)     │─────────▶│  (Spring Boot)    │
    │  Port: 5000    │          │   Port: 8181      │
    └────────────────┘          └────────┬──────────┘
                                         │
                                ┌────────▼──────────┐
                                │  PostgreSQL DB    │
                                │  192.168.10.248   │
                                └───────────────────┘
```

### Kubernetes Resources
- **Namespace**: `metrics-dashboard`
- **Deployments**: Backend (2 replicas), Frontend (2 replicas)
- **Services**: ClusterIP for internal communication
- **ConfigMaps**: Non-sensitive configuration
- **Secrets**: Database passwords, API keys
- **Ingress**: External access routing

---

## Prerequisites

### Required Tools
```bash
# kubectl - Kubernetes CLI
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
kubectl version --client

# kustomize (built into kubectl 1.14+)
kubectl kustomize --help

# Optional: Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### Cluster Requirements
- Kubernetes version: **1.25+**
- Minimum nodes: **3** (for production)
- Node resources per node:
  - CPU: **4 cores**
  - Memory: **8 GB RAM**
  - Storage: **50 GB SSD**

### Network Requirements
- **Ingress Controller** installed (NGINX recommended)
- **External IP** or LoadBalancer support
- **DNS** configured for domain routing
- **Access to external PostgreSQL** (192.168.10.248:5432)

---

## Kubernetes Resources

### Directory Structure
```
k8s/
├── base/
│   ├── namespace.yaml              # Namespace definition
│   ├── configmap.yaml             # Application configuration
│   ├── secret.yaml                # Sensitive data (passwords)
│   ├── backend-deployment.yaml    # Backend deployment
│   ├── backend-service.yaml       # Backend service
│   ├── frontend-deployment.yaml   # Frontend deployment
│   ├── frontend-service.yaml      # Frontend service
│   ├── ingress.yaml               # Ingress routing rules
│   └── kustomization.yaml         # Base kustomization
│
└── overlays/
    └── production/
        └── kustomization.yaml     # Production overrides
```

### Resource Specifications

#### Namespace
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: metrics-dashboard
```

#### ConfigMap
Stores non-sensitive configuration:
- Database connection URL
- SMTP host/port
- Scheduler cron expressions
- Connection pool settings
- Frontend API URL

#### Secret
Stores sensitive data (base64 encoded):
- Database password
- Email password

#### Deployments
- **Backend**: 2 replicas, 512Mi-1Gi memory, 250m-1000m CPU
- **Frontend**: 2 replicas, 256Mi-512Mi memory, 100m-500m CPU

#### Services
- **Type**: ClusterIP (internal)
- **Ports**: Backend (8181), Frontend (5000)

#### Ingress
- **Host**: metrics-dashboard.yourdomain.com
- **TLS**: Enabled with Let's Encrypt
- **Paths**:
  - `/api/*` → Backend
  - `/swagger-ui/*` → Backend
  - `/*` → Frontend

---

## Deployment Methods

### Method 1: Manual kubectl Apply

#### Step 1: Prepare Configuration
```bash
cd k8s/base

# Update secret with actual passwords
cat > secret.yaml <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: metrics-dashboard-secrets
  namespace: metrics-dashboard
type: Opaque
stringData:
  SPRING_DATASOURCE_PASSWORD: "xaX48cgDymcHwq5"
  SPRING_MAIL_PASSWORD: "your-email-app-password"
EOF
```

#### Step 2: Update Image References
Edit `backend-deployment.yaml` and `frontend-deployment.yaml`:
```yaml
spec:
  template:
    spec:
      containers:
      - name: backend
        image: your-registry.azurecr.io/metrics-dashboard-backend:v1.0.0
```

#### Step 3: Deploy Resources
```bash
# Create namespace
kubectl apply -f k8s/base/namespace.yaml

# Apply all resources
kubectl apply -f k8s/base/

# Or use kustomize
kubectl apply -k k8s/base
```

#### Step 4: Verify Deployment
```bash
# Check all resources
kubectl get all -n metrics-dashboard

# Check pods
kubectl get pods -n metrics-dashboard
NAME                                        READY   STATUS    RESTARTS   AGE
metrics-dashboard-backend-7d8f9b5c-abc12   1/1     Running   0          2m
metrics-dashboard-backend-7d8f9b5c-def34   1/1     Running   0          2m
metrics-dashboard-frontend-6c7d8e9f-xyz89  1/1     Running   0          2m
metrics-dashboard-frontend-6c7d8e9f-uvw12  1/1     Running   0          2m

# Check services
kubectl get svc -n metrics-dashboard

# Check ingress
kubectl get ingress -n metrics-dashboard
```

### Method 2: Kustomize Overlays

#### Production Deployment
```bash
# Preview manifests
kubectl kustomize k8s/overlays/production

# Apply production configuration
kubectl apply -k k8s/overlays/production

# Watch rollout
kubectl rollout status deployment/metrics-dashboard-backend -n metrics-dashboard
kubectl rollout status deployment/metrics-dashboard-frontend -n metrics-dashboard
```

### Method 3: ArgoCD GitOps

#### Install ArgoCD
```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for pods
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=5m

# Get admin password
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d

# Access UI (port-forward)
kubectl port-forward svc/argocd-server -n argocd 8181:443
```

#### Deploy Application
```bash
# Apply ArgoCD Application
kubectl apply -f deployment/argocd-application.yaml

# Or via CLI
argocd login localhost:8181

argocd app create metrics-dashboard \
  --repo https://github.com/your-org/metrics-dashboard.git \
  --path k8s/overlays/production \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace metrics-dashboard \
  --sync-policy automated

# Sync application
argocd app sync metrics-dashboard

# Monitor
argocd app wait metrics-dashboard --health
```

---

## Configuration Management

### Update ConfigMap
```bash
# Edit ConfigMap
kubectl edit configmap metrics-dashboard-config -n metrics-dashboard

# Or apply updated file
kubectl apply -f k8s/base/configmap.yaml

# Restart deployments to pick up changes
kubectl rollout restart deployment/metrics-dashboard-backend -n metrics-dashboard
```

### Update Secrets
```bash
# Update secret
kubectl create secret generic metrics-dashboard-secrets \
  --from-literal=SPRING_DATASOURCE_PASSWORD=new-password \
  --from-literal=SPRING_MAIL_PASSWORD=new-email-password \
  -n metrics-dashboard \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart pods
kubectl rollout restart deployment/metrics-dashboard-backend -n metrics-dashboard
```

### Runtime Configuration (app_config table)
Application also supports runtime configuration via database:
```bash
# Access API to update config
kubectl port-forward svc/metrics-dashboard-backend -n metrics-dashboard 8181:8181

curl -X PUT http://localhost:8181/api/config/SCHEDULER_CRON_COMMERCIAL \
  -H "Content-Type: application/json" \
  -d '{"configValue": "0 */30 * * * *", "description": "Every 30 minutes"}'
```

---

## Monitoring & Logging

### View Logs
```bash
# Backend logs
kubectl logs -f -n metrics-dashboard -l component=backend

# Frontend logs
kubectl logs -f -n metrics-dashboard -l component=frontend

# Specific pod
kubectl logs -f -n metrics-dashboard <pod-name>

# Previous container logs (if crashed)
kubectl logs -n metrics-dashboard <pod-name> --previous
```

### Resource Usage
```bash
# Pod resource usage
kubectl top pods -n metrics-dashboard

# Node usage
kubectl top nodes
```

### Events
```bash
# Recent events
kubectl get events -n metrics-dashboard --sort-by='.lastTimestamp'

# Watch events
kubectl get events -n metrics-dashboard --watch
```

### Health Checks
```bash
# Backend health
kubectl port-forward svc/metrics-dashboard-backend -n metrics-dashboard 8181:8181
curl http://localhost:8181/actuator/health

# Frontend health
kubectl port-forward svc/metrics-dashboard-frontend -n metrics-dashboard 5000:5000
curl http://localhost:5000
```

---

## Scaling & Performance

### Manual Scaling
```bash
# Scale backend
kubectl scale deployment metrics-dashboard-backend --replicas=5 -n metrics-dashboard

# Scale frontend
kubectl scale deployment metrics-dashboard-frontend --replicas=3 -n metrics-dashboard
```

### Horizontal Pod Autoscaler (HPA)
```bash
# Create HPA for backend (CPU-based)
kubectl autoscale deployment metrics-dashboard-backend \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n metrics-dashboard

# Create HPA for frontend
kubectl autoscale deployment metrics-dashboard-frontend \
  --cpu-percent=60 \
  --min=2 \
  --max=5 \
  -n metrics-dashboard

# Check HPA status
kubectl get hpa -n metrics-dashboard
```

### Resource Limits
Edit deployments to adjust resource limits:
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

---

## Security Best Practices

### 1. Use Secrets for Sensitive Data
```bash
# Never commit secrets to Git
# Use sealed-secrets or external secret managers
kubectl create secret generic db-credentials \
  --from-literal=password=$(openssl rand -base64 32) \
  -n metrics-dashboard
```

### 2. Network Policies
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-network-policy
  namespace: metrics-dashboard
spec:
  podSelector:
    matchLabels:
      component: backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          component: frontend
    ports:
    - protocol: TCP
      port: 8181
```

### 3. RBAC
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: metrics-dashboard-role
  namespace: metrics-dashboard
rules:
- apiGroups: [""]
  resources: ["pods", "services"]
  verbs: ["get", "list"]
```

### 4. Pod Security Standards
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: backend
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
  containers:
  - name: backend
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
```

---

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting (ImagePullBackOff)
```bash
# Check pod events
kubectl describe pod <pod-name> -n metrics-dashboard

# Verify image exists
docker pull your-registry/metrics-dashboard-backend:v1.0.0

# Check image pull secrets
kubectl get secrets -n metrics-dashboard
```

#### 2. CrashLoopBackOff
```bash
# View logs
kubectl logs <pod-name> -n metrics-dashboard

# Common causes:
# - Database connection failure → Check DB credentials
# - Missing environment variables → Check ConfigMap/Secret
# - Port already in use → Check port conflicts
```

#### 3. Service Not Accessible
```bash
# Check service
kubectl get svc -n metrics-dashboard
kubectl describe svc metrics-dashboard-backend -n metrics-dashboard

# Test from another pod
kubectl run -it --rm debug --image=busybox --restart=Never -- sh
wget -O- http://metrics-dashboard-backend.metrics-dashboard:8181/actuator/health
```

#### 4. Database Connection Failed
```bash
# Test database connectivity
kubectl run -it --rm psql-test --image=postgres:13 --restart=Never -- \
  psql -h 192.168.10.248 -U postgres -d inticsdev -c "SELECT 1"

# Check network policies
kubectl get networkpolicies -n metrics-dashboard
```

#### 5. Ingress Not Working
```bash
# Check ingress
kubectl get ingress -n metrics-dashboard
kubectl describe ingress metrics-dashboard-ingress -n metrics-dashboard

# Check ingress controller
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx <ingress-controller-pod>

# Test backend directly
kubectl port-forward svc/metrics-dashboard-backend -n metrics-dashboard 8181:8181
curl http://localhost:8181/api/metrics/daily
```

### Debug Commands

```bash
# Execute shell in pod
kubectl exec -it <pod-name> -n metrics-dashboard -- /bin/bash

# Copy files from pod
kubectl cp metrics-dashboard/<pod-name>:/path/to/file ./local-file

# Port forward for debugging
kubectl port-forward <pod-name> -n metrics-dashboard 8181:8181

# Get full pod YAML
kubectl get pod <pod-name> -n metrics-dashboard -o yaml

# Delete and recreate pod
kubectl delete pod <pod-name> -n metrics-dashboard
```

---

## Rollback & Updates

### Rolling Update
```bash
# Update image
kubectl set image deployment/metrics-dashboard-backend \
  backend=your-registry/metrics-dashboard-backend:v1.1.0 \
  -n metrics-dashboard

# Watch rollout
kubectl rollout status deployment/metrics-dashboard-backend -n metrics-dashboard
```

### Rollback
```bash
# View rollout history
kubectl rollout history deployment/metrics-dashboard-backend -n metrics-dashboard

# Rollback to previous version
kubectl rollout undo deployment/metrics-dashboard-backend -n metrics-dashboard

# Rollback to specific revision
kubectl rollout undo deployment/metrics-dashboard-backend \
  --to-revision=2 \
  -n metrics-dashboard
```

---

## Backup & Disaster Recovery

### Backup Configuration
```bash
# Export all resources
kubectl get all -n metrics-dashboard -o yaml > backup-$(date +%Y%m%d).yaml

# Backup specific resources
kubectl get configmap,secret -n metrics-dashboard -o yaml > config-backup.yaml
```

### Restore
```bash
# Restore from backup
kubectl apply -f backup-20251021.yaml
```

---

## Performance Tuning

### JVM Tuning (Backend)
```yaml
env:
- name: JAVA_OPTS
  value: "-Xms512m -Xmx1024m -XX:+UseG1GC"
```

### Connection Pool Tuning
Update ConfigMap:
```yaml
HIKARI_MAX_POOL_SIZE: "20"
HIKARI_MIN_IDLE: "5"
HIKARI_CONNECTION_TIMEOUT: "20000"
```

---

## Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kustomize Documentation](https://kustomize.io/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)

---

## Support

For issues or questions:
- Check logs: `kubectl logs -f <pod-name> -n metrics-dashboard`
- Review events: `kubectl get events -n metrics-dashboard`
- Contact: support@yourdomain.com
