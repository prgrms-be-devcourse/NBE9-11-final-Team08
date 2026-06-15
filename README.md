# NBE9-11-final-Team08
2026 프로그래머스 데브코스 백엔드 최종 프로젝트 레포지토리 입니다

## Local Docker

1. Create the local environment file.

   ```bash
   cp infra/compose/.env.example infra/compose/.env
   ```

2. Replace every `change-me` value in `infra/compose/.env`.

3. Start MySQL, the backend, and Nginx.

   ```bash
   docker compose --env-file infra/compose/.env \
     -f infra/compose/compose.yaml up --build -d
   docker compose --env-file infra/compose/.env \
     -f infra/compose/compose.yaml ps
   curl http://localhost/actuator/health
   ```

Only Nginx ports `80` and `443` are published. MySQL and the backend stay inside the
Compose networks. Local Docker uses HTTP by default.

## EC2 Deployment

Prerequisites:

- AWS credentials available to Terraform
- Terraform 1.7 or newer
- An SSH key pair on the local machine
- An external DNS domain
- A public GHCR package for this repository

Create the infrastructure:

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Set admin_cidr and public_key_path.
terraform init
terraform apply
```

Upload the deployment files:

```bash
./infra/scripts/deploy.sh <elastic-ip> <ssh-private-key> [image-tag]
```

The first run creates `/opt/team08/compose/.env` on the server and stops. Connect
over SSH, fill in that file, set `IS_COOKIE_SECURE=true`, and run the deploy
command again. Create an external DNS `A` record pointing the API domain to the
Terraform `elastic_ip` output.

After DNS propagation, issue the certificate and start HTTPS:

```bash
ssh -i <ssh-private-key> ubuntu@<elastic-ip>
cd /opt/team08
./scripts/bootstrap-https.sh
```

The bootstrap installs a twice-daily Certbot renewal cron job. Terraform state and
production secrets remain local and are not committed.
