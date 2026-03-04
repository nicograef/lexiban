# AWS Cloud-Native Deployment — Zusammenfassung

> **Status: Umgesetzt.** Docker Compose → AWS managed services via CDK.
> Siehe `infra/` für CDK-Stacks, `StreamLambdaHandler.java` für Lambda-Adapter.

---

## Service-Mapping

| Docker Compose        | AWS                                     |
| --------------------- | --------------------------------------- |
| Nginx + React SPA     | S3 + CloudFront                         |
| Spring Boot Container | Lambda + API Gateway (HTTP) + SnapStart |
| PostgreSQL Container  | RDS PostgreSQL + RDS Proxy              |
| Nginx Reverse Proxy   | CloudFront (HTTPS)                      |
| docker-compose.yml    | AWS CDK (TypeScript)                    |

## Architecture

```
CloudFront → /*       → S3 (SPA)
           → /api/*   → API Gateway → Lambda (Spring Boot) → RDS Proxy → RDS PostgreSQL
```

All backend resources in VPC with private subnets + NAT Gateway.

## CDK Stacks (`infra/`)

| Stack               | Resources                                     |
| ------------------- | --------------------------------------------- |
| `network-stack.ts`  | VPC, subnets, NAT Gateway                     |
| `database-stack.ts` | RDS PostgreSQL, RDS Proxy, Secrets Manager    |
| `backend-stack.ts`  | Lambda (Java 21, SnapStart), API Gateway HTTP |
| `frontend-stack.ts` | S3 bucket, CloudFront distribution            |

## Backend Changes

1. **Maven dependency**: `aws-serverless-java-container-springboot3`
2. **`StreamLambdaHandler.java`**: bridges API Gateway events → Spring Boot DispatcherServlet
3. **`application-aws.properties`**: RDS Proxy connection, `hikari.maximum-pool-size=1`

**Frontend**: zero changes (relative `/api/ibans` path works via CloudFront routing).

## Deploy / Teardown

```bash
cdk deploy --all -c stage=dev    # deploy
cdk destroy --all -c stage=dev   # teardown
```

## Key Gotchas

- **SnapStart + DB**: set `maximum-pool-size=1` — RDS Proxy handles pooling
- **CORS**: not needed — CloudFront serves SPA and API from same origin
- **Flyway**: runs on first Lambda invocation (~5-10s cold start)
- **NAT Gateway**: required for openiban.com calls from VPC Lambda

## Lokale AWS-Credentials (Codespaces / Dev Container)

`aws sso login` funktioniert in Codespaces **nicht** (OAuth-Callback kann den Container nicht erreichen → 403). Stattdessen: IAM Access Key + `aws configure`.

### Einrichtung

1. **IAM User mit Access Key erstellen** (einmalig in der AWS Console):
   - → **IAM → Users → Create user** (z. B. `lexiban-dev`)
   - Permissions: `AdministratorAccess` (oder eingeschränkt nach Bedarf)
   - → **Security credentials → Create access key** → Use case: "Command Line Interface"
   - Access Key ID + Secret Access Key kopieren

2. **Credentials im Codespace konfigurieren:**

   ```bash
   aws configure
   ```

   Eingaben:
   | Prompt | Wert |
   | ----------------------- | ----------------------------- |
   | AWS Access Key ID | `AKIA...` (aus Schritt 1) |
   | AWS Secret Access Key | `wJal...` (aus Schritt 1) |
   | Default region name | `eu-central-1` |
   | Default output format | `json` |

   Das schreibt `~/.aws/credentials` und `~/.aws/config`. Alternativ kann eine projektlokale `.aws/credentials`-Datei genutzt werden (ist bereits in `.gitignore`).

3. **Verbindung testen:**

   ```bash
   aws sts get-caller-identity
   ```

> **Hinweis:** Codespaces-Instanzen sind kurzlebig — nach Rebuild muss `aws configure` erneut ausgeführt werden. Tipp: Credentials als **Codespaces Secrets** hinterlegen (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`), dann sind sie automatisch als Umgebungsvariablen verfügbar.

---

## CI/CD Setup (GitHub Actions → AWS)

Die Pipeline (`.github/workflows/deploy.yml`) deployt automatisch bei Push auf `main`. Voraussetzung: einmalige Einrichtung von OIDC, IAM-Rolle und CDK Bootstrap.

### 1. OIDC Identity Provider anlegen

GitHub Actions authentifiziert sich über OIDC — keine langlebigen Access Keys nötig.

**AWS CLI:**

```bash
# Account-ID ermitteln
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# OIDC Provider erstellen
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

**Oder via AWS Console:**

1. → **IAM → Identity providers → Add provider**
2. Provider type: **OpenID Connect**
3. Provider URL: `https://token.actions.githubusercontent.com`
4. Audience: `sts.amazonaws.com`
5. **Add provider** klicken

### 2. IAM-Rolle erstellen

**AWS CLI:**

```bash
# Trust Policy als Datei anlegen
cat > /tmp/trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:nicograef/iban:ref:refs/heads/main"
        }
      }
    }
  ]
}
EOF

# Rolle erstellen
aws iam create-role \
  --role-name GitHubActions-Lexiban-Deploy \
  --assume-role-policy-document file:///tmp/trust-policy.json

# AdministratorAccess anhängen (oder eingeschränkte Policy für Prod)
aws iam attach-role-policy \
  --role-name GitHubActions-Lexiban-Deploy \
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess

# Rollen-ARN ausgeben (wird in Schritt 3 benötigt)
aws iam get-role --role-name GitHubActions-Lexiban-Deploy \
  --query Role.Arn --output text
```

**Oder via AWS Console:**

1. → **IAM → Roles → Create role**
2. Trusted entity type: **Web identity**
3. Identity provider: `token.actions.githubusercontent.com`
4. Audience: `sts.amazonaws.com`
5. **Next** → Policy: `AdministratorAccess` auswählen
6. Role name: `GitHubActions-Lexiban-Deploy` → **Create role**
7. Rolle öffnen → **Trust relationships → Edit trust policy**
8. Condition-Block ergänzen:
   ```json
   "StringLike": {
     "token.actions.githubusercontent.com:sub": "repo:nicograef/iban:ref:refs/heads/main"
   }
   ```
9. **Update policy** → Rollen-ARN kopieren

### 3. GitHub Secret setzen

**GitHub CLI:**

```bash
gh secret set AWS_ROLE_ARN \
  --repo nicograef/iban \
  --body "arn:aws:iam::<ACCOUNT_ID>:role/GitHubActions-Lexiban-Deploy"
```

**Oder via GitHub UI:**

1. → **Repository → Settings → Secrets and variables → Actions**
2. **New repository secret**
3. Name: `AWS_ROLE_ARN`
4. Value: `arn:aws:iam::<ACCOUNT_ID>:role/GitHubActions-Lexiban-Deploy`
5. **Add secret**

### 4. CDK Bootstrap (einmalig)

CDK benötigt einmalig einen Bootstrap-Stack im Ziel-Account/Region.

**Wichtig:** CDK synthetisiert beim Bootstrap alle Stacks und benötigt dabei die Build-Artefakte (Backend-JAR + Frontend-Dist). Der Makefile-Target erledigt beides automatisch:

```bash
make aws-bootstrap
```

Das führt nacheinander aus:

1. `cd backend && ./mvnw package -DskipTests -B` — baut `target/lexiban-0.0.1-SNAPSHOT.jar`
2. `cd frontend && pnpm install && pnpm build` — baut `dist/`
3. `cd infra && npx cdk bootstrap aws://<ACCOUNT_ID>/eu-central-1`

> **Hinweis:** AWS-Credentials müssen vorher eingerichtet sein (siehe Abschnitt „Lokale AWS-Credentials" oben), oder AWS CloudShell verwenden.

### 5. Deploy auslösen

Push auf `main` startet die Pipeline automatisch:

1. **CI**: Backend (`mvnw verify`) + Frontend (lint, build, test) parallel
2. **Deploy**: JAR + Frontend bauen, dann `cdk deploy --all -c stage=prod`

```bash
git push origin main
```

### Kosten-Hinweis

| Ressource                           | ca. Kosten/Monat      |
| ----------------------------------- | --------------------- |
| NAT Gateway                         | ~$32                  |
| RDS t4g.micro                       | ~$12                  |
| RDS Proxy                           | ~$10                  |
| Lambda, API Gateway, S3, CloudFront | Pay-per-use (minimal) |

Teardown: `cd infra && npx cdk destroy --all -c stage=prod`
