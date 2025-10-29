# Deployment Files

This directory contains all deployment configuration files for the Metrics Dashboard application.

## Files

### ArgoCD
- **argocd-application.yaml**: ArgoCD Application manifest for GitOps deployment

## Quick Start

### Deploy with ArgoCD
```bash
# Apply ArgoCD Application
kubectl apply -f argocd-application.yaml

# Sync application
argocd app sync metrics-dashboard
```

## Configuration

Before deploying, update the following in `argocd-application.yaml`:
- `spec.source.repoURL`: Your Git repository URL
- `spec.source.targetRevision`: Target branch (default: main)

## Documentation

See parent directory documentation:
- **DEPLOYMENT.md**: Complete deployment guide
- **KUBERNETES.md**: Kubernetes-specific deployment guide
- **Jenkinsfile**: CI/CD pipeline configuration
