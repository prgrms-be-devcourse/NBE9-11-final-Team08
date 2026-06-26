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

Create the infrastructure. Terraform creates one EC2 host:

- app host: t3.medium, runs Nginx, Certbot, MySQL, Redis, and Spring blue-green containers

The security group opens only SSH, HTTP, and HTTPS. Spring blue-green ports are
bound to `127.0.0.1` on the host and are proxied through Nginx.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Set admin_cidr and public_key_path.
terraform init
terraform apply
```

Deploy the first backend image with blue-green:

```bash
./infra/scripts/deploy-bluegreen.sh <server-elastic-ip> <ssh-private-key> <image-tag>
```

The first run creates `/opt/team08/compose/.env` on the server and stops. Connect
over SSH, fill in that file, set `IS_COOKIE_SECURE=true`, and run the deploy
command again.

Create an external DNS `A` record pointing the API domain to the Terraform
`server_elastic_ip` output. In `/opt/team08/compose/.env`, set `DOMAIN`,
`CERTBOT_EMAIL`, and keep `BACKEND_UPSTREAM=backend-blue:8080` unless the active
color is already green.

After DNS propagation, issue the certificate and start HTTPS:

```bash
ssh -i <ssh-private-key> ubuntu@<server-elastic-ip>
cd /opt/team08
./scripts/bootstrap-https.sh
```

The bootstrap installs a twice-daily Certbot renewal cron job. Terraform state and
production secrets remain local and are not committed.

## Blue-Green Deployment

The server runs Spring as `backend-blue` on host-local `8081` and
`backend-green` on host-local `8082`. Deploying a new image starts the inactive
color, waits for `/actuator/health`, switches the local Nginx upstream to
`backend-blue:8080` or `backend-green:8080`, then records the active color.

Manual blue-green deployment:

```bash
./infra/scripts/deploy-bluegreen.sh \
  <server-elastic-ip> <ssh-private-key> <image-tag>
```

GitHub Actions can do the same from the `Backend Image` workflow by setting
`deploy=true`. Configure these repository secrets first:

- `PROD_APP_PUBLIC_IP`
- `PROD_SSH_PRIVATE_KEY`
