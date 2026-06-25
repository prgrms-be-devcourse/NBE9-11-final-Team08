# NBE9-11-final-Team08
2026 프로그래머스 데브코스 백엔드 최종 프로젝트 레포지토리 입니다

## Local Docker

1. Create the local environment file.

   ```bash
   cp infra/compose/.env.example infra/compose/.env
   ```

2. Replace every `change-me` value in `infra/compose/.env`.

3. Start MySQL and the backend.

   ```bash
   docker compose --env-file infra/compose/.env \
     -f infra/compose/compose.yaml up --build -d
   docker compose --env-file infra/compose/.env \
     -f infra/compose/compose.yaml ps
   curl http://localhost:8080/actuator/health
   ```

MySQL and Redis stay inside the Compose network. The backend is exposed on
`BACKEND_PORT` for local development and performance tests.

## EC2 Deployment

Prerequisites:

- AWS credentials available to Terraform
- Terraform 1.7 or newer
- An SSH key pair on the local machine
- An external DNS domain
- A public GHCR package for this repository

Create the infrastructure. Terraform creates two EC2 hosts:

- edge host: t3.small, runs Nginx and Certbot
- app host: t3.medium, runs MySQL, Redis, and Spring

The app security group allows Spring port `8080` only from the edge security
group.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Set admin_cidr and public_key_path.
terraform init
terraform apply
```

Upload and start the app server first:

```bash
./infra/scripts/deploy.sh <app-public-ip> <ssh-private-key> [image-tag]
```

The first run creates `/opt/team08/compose/.env` on the server and stops. Connect
over SSH, fill in that file, set `IS_COOKIE_SECURE=true`, and run the deploy
command again.

Upload and start the edge server:

```bash
./infra/scripts/deploy-edge.sh <edge-elastic-ip> <ssh-private-key>
```

On the edge server, fill `/opt/team08/compose/.env` with `DOMAIN`,
`CERTBOT_EMAIL`, and `BACKEND_UPSTREAM=<app-private-ip>:8080`. Create an external
DNS `A` record pointing the API domain to the Terraform `edge_elastic_ip` output.

After DNS propagation, issue the certificate and start HTTPS:

```bash
ssh -i <ssh-private-key> ubuntu@<edge-elastic-ip>
cd /opt/team08
./scripts/bootstrap-https.sh
```

The bootstrap installs a twice-daily Certbot renewal cron job. Terraform state and
production secrets remain local and are not committed.
