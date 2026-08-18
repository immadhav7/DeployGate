# DeployGate

**Deployment request management tool with role-based approval workflow** — Spring Boot, PostgreSQL

DeployGate helps developer and deployment teams track the files involved in a release, reducing the risk of missed files during deployment. Beyond the application itself, this repository contains a complete, end-to-end DevOps pipeline: source control → CI/CD → containerization → orchestration → monitoring, all built and validated on real infrastructure.

---

## Application

- **Stack:** Java, Spring Boot, Maven, Thymeleaf, PostgreSQL (Spring Data JPA / Hibernate)
- **Core features:**
  - Single Upload — validate and merge one or more Excel files into a combined dataset
  - Bulk Upload — batch processing of multiple files
  - Deployment Request module — structured tracking for a deployment request
  - Role model (developer / deployer / reviewer / admin) — admin role currently implemented
- **Port:** `8081`

---

## Pipeline Overview

```
GitHub (this repo)
   │  push
   ▼
Jenkins  →  Checkout → Build → Test → SonarCloud Analysis → Quality Gate → Package → Docker Build
   │  push image
   ▼
Docker Hub  (immadhav7/deploygate)
   │  pull
   ▼
Kubernetes (k3s)  →  Deployment + Service (app)  +  Deployment + Service + PVC (PostgreSQL)
   │
   ▼
Prometheus + Grafana  (cluster monitoring)
```

---

## Repository Structure

| Folder | Contents |
|---|---|
| `src/` | Application source code |
| `Dockerfile`, `docker-compose.yml` | Container build definition |
| `Jenkinsfile` | Declarative Jenkins CI/CD pipeline |
| `k8s/` | Raw Kubernetes manifests (Deployments, Services, ConfigMap, Secret, PVC) |
| `helm/` | Helm chart wrapping the manifests above, parameterized via `values.yaml` |
| `monitoring/` | Helm values for Prometheus and Grafana (lightweight, resource-capped setup) |
| `ansible/` | Playbooks for provisioning (Docker, Jenkins, k3s+Helm) and operations (deploy, restart) |
| `terraform/` | IaC for a one-time AWS EKS proof-of-concept deployment (VPC, IAM, EKS cluster + node group) |

---

## CI/CD Pipeline (Jenkins)

Every push triggers:

1. **Checkout** — pull latest from GitHub
2. **Build** — `mvn clean compile`
3. **Test** — `mvn test` (37 unit tests, results published via JUnit)
4. **SonarCloud Analysis** — static analysis for security, reliability, and maintainability
5. **Quality Gate** — gate check against SonarCloud results
6. **Package** — `mvn package -DskipTests`
7. **Docker Build** — build and tag the image, push to Docker Hub

---

## Kubernetes Deployment

The application runs on a self-managed **k3s** cluster on AWS EC2, chosen deliberately over a continuously-running AWS EKS cluster to keep the project at zero ongoing cloud cost — k3s is a fully conformant Kubernetes distribution, so every manifest and Helm chart here works unmodified on EKS.

**Deploy via raw manifests:**
```bash
kubectl apply -f k8s/
```

**Deploy via Helm (recommended):**
```bash
helm install deploygate ./helm
```

Config (DB URL, username) is supplied via a `ConfigMap`; the database password is supplied via a `Secret`, referenced with `secretKeyRef` rather than hardcoded. PostgreSQL runs in-cluster as its own `Deployment` backed by a `PersistentVolumeClaim`.

---

## Monitoring (Prometheus + Grafana)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts

helm install prometheus prometheus-community/prometheus -f monitoring/prometheus-values.yaml \
  --namespace monitoring --create-namespace

helm install grafana grafana/grafana -f monitoring/grafana-values.yaml \
  --namespace monitoring
```

A lightweight, hand-assembled Prometheus + Grafana setup is used instead of the full `kube-prometheus-stack` chart — the all-in-one chart (Operator, kube-state-metrics, Alertmanager included) repeatedly exhausted memory on a small single-node cluster. The lighter setup (plain Prometheus server + node-exporter + standalone Grafana) uses a fraction of the resources and was verified end-to-end via live scrape-target queries.

---

## Infrastructure as Code (Terraform)

`terraform/` defines the AWS resources for a one-time EKS proof-of-concept run: a VPC with public subnets across two AZs, IAM roles for the cluster and node group, an EKS cluster, and a managed node group.

```bash
cd terraform
terraform init
terraform plan
terraform apply   # provisions real, billable AWS resources
```

This is intentionally **not applied continuously** — EKS's control plane has an hourly cost with no free tier, so it's provisioned only for a short, deliberate verification session and destroyed immediately after (`terraform destroy`).

---

## Configuration Management (Ansible)

Playbooks in `ansible/` cover both provisioning and day-to-day operations:

| Playbook | Purpose |
|---|---|
| `install-docker.yml` | Installs Docker Engine |
| `install-jenkins.yml` | Installs Java 21 and Jenkins |
| `install-k3s.yml` | Installs k3s + Helm, including the swap-file fix required for stable operation on small instances |
| `deploy-app.yml` | Runs `helm upgrade --install` for the application |
| `restart-services.yml` | Safely restarts Jenkins and k3s with readiness checks |

```bash
ansible-playbook -i ansible/inventory.ini ansible/install-docker.yml --limit jenkins
```

---

## Notable Engineering Decisions

- **SonarCloud instead of self-hosted SonarQube** — avoided repeating a resource-exhaustion fight on the same small Jenkins instance for a component whose hosting location isn't the skill being demonstrated.
- **k3s instead of continuously-running EKS** — real Kubernetes, zero ongoing cost; EKS reserved for a single time-boxed proof run.
- **Swap configuration on the k3s node** — diagnosed and fixed a Kubernetes API hang caused by memory pressure and no swap, root-caused via `top`/`free -h`/`iostat` rather than guesswork.
- **EBS volume + instance resizing** — resized a too-small root volume (6.7GB → 20GB) and instance type (t3.micro → t3.small) after hitting real capacity limits during Ansible provisioning and monitoring rollout.
- **Lightweight Prometheus + Grafana over `kube-prometheus-stack`** — the standard all-in-one chart repeatedly overwhelmed the cluster's memory; a hand-assembled, right-sized setup was chosen instead and verified working end-to-end.

---

## Tech Stack

**Application:** Java · Spring Boot · Maven · Thymeleaf · PostgreSQL
**CI/CD:** Jenkins · SonarCloud
**Containers:** Docker · Docker Hub
**Orchestration:** Kubernetes (k3s) · Helm
**IaC / Config Management:** Terraform · Ansible
**Monitoring:** Prometheus · Grafana
**Cloud:** AWS (EC2, EKS, IAM, VPC)
