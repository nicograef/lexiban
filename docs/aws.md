# AWS Cloud-Native Deployment Plan (CDK + GitHub Actions)

> Deploy Lexiban **without Docker** on AWS using managed services and CDK as IaC.
> Optimized for **simplicity** — this is a demo that runs for a few days.

---

## 1. Overview

Replace each Docker Compose service with a managed AWS equivalent:

| Current (Docker Compose)     | AWS Replacement                       |
| ---------------------------- | ------------------------------------- |
| Nginx serving React SPA      | **S3 + CloudFront** (static hosting)  |
| Spring Boot JAR in container | **Lambda + API Gateway** (serverless) |
| PostgreSQL 17 container      | **RDS PostgreSQL** (managed)          |
| Nginx reverse proxy (TLS)    | **CloudFront** (HTTPS built-in)       |
| Docker Compose               | **AWS CDK** (TypeScript IaC)          |
| Manual VPS deploy            | **GitHub Actions CI/CD**              |

**Decisions:**

- **Region:** `eu-central-1` (Frankfurt).
- **Domain:** None — CloudFront auto-generated URL (`d1234abcd.cloudfront.net`) is sufficient.
- **openiban.com:** Kept — NAT Gateway required but cost is irrelevant for a few-day demo.
- **Environments:** Dev + Prod stacks (same architecture, different config).

### Why Lambda + S3 + RDS?

- **S3 + CloudFront**: The canonical way to host a static SPA on AWS. Vite outputs `dist/` (HTML/CSS/JS) — upload to S3, serve via CloudFront with HTTPS.
- **Lambda + API Gateway**: The backend is 2 REST endpoints (POST + GET), stateless, request-scoped — a textbook Lambda use case. `aws-serverless-java-container-springboot3` runs the existing Spring Boot app on Lambda with one new class. **SnapStart** (Java 21) reduces cold starts to ~200ms.
- **RDS PostgreSQL**: Managed Postgres, compatible with Flyway. Same engine as current Docker setup. **RDS Proxy** handles Lambda connection pooling.
- **CDK (TypeScript)**: Type-safe IaC, good IDE support. Fits existing frontend TS skills.

> **Future improvement:** For production with a custom domain, add Route 53 + ACM certificate. The CDK distribution construct supports this with a few extra lines.

---

## 2. Architecture

```
               ┌──────────────────────────────┐
               │       CloudFront CDN          │
               │    (HTTPS, d1234.cf.net)      │
               └──────┬─────────────┬─────────┘
                      │             │
               /*  (SPA)      /api/* (proxy)
                      │             │
           ┌──────────▼──┐  ┌──────▼───────────┐
           │  S3 Bucket  │  │  API Gateway HTTP │
           │  (static    │  │  (→ Lambda)       │
           │   React)    │  └──────┬───────────┘
           └─────────────┘         │
                            ┌──────▼───────────┐
                            │  Lambda Function  │
                            │  (Spring Boot +   │
                            │   SnapStart)      │
                            └──────┬───────────┘
                                   │
                            ┌──────▼───────────┐
                            │   RDS Proxy       │
                            └──────┬───────────┘
                                   │
                            ┌──────▼───────────┐
                            │  RDS PostgreSQL   │
                            │  (db.t4g.micro)   │
                            └──────────────────┘
```

All backend resources live in a **VPC** with private subnets + NAT Gateway (Lambda needs internet for openiban.com).

---

## 3. CDK Project Structure

```
infra/
├── bin/
│   └── lexiban.ts              # CDK app entry — instantiates stacks per environment
├── lib/
│   ├── network-stack.ts        # VPC + subnets + NAT Gateway
│   ├── database-stack.ts       # RDS + RDS Proxy + Secrets Manager
│   ├── backend-stack.ts        # Lambda + API Gateway
│   └── frontend-stack.ts       # S3 + CloudFront (routes /api/* → API Gateway)
├── cdk.json
├── package.json
└── tsconfig.json
```

**Dev vs. Prod** is handled via CDK context / props — same stacks, different parameters:

| Parameter        | Dev                     | Prod                         |
| ---------------- | ----------------------- | ---------------------------- |
| RDS instance     | `db.t4g.micro`          | `db.t4g.small` (or multi-AZ) |
| RDS deletion     | `RemovalPolicy.DESTROY` | `RemovalPolicy.RETAIN`       |
| Lambda memory    | 512 MB                  | 1024 MB                      |
| NAT Gateways     | 1                       | 2 (HA)                       |
| CloudFront price | `PRICE_CLASS_100`       | `PRICE_CLASS_200`            |

---

## 4. Implementation Details

### 4.1 Stack: Network (`network-stack.ts`)

```typescript
import * as ec2 from "aws-cdk-lib/aws-ec2";

const vpc = new ec2.Vpc(this, "Vpc", {
  maxAzs: 2,
  natGateways: 1,
  subnetConfiguration: [
    { name: "public", subnetType: ec2.SubnetType.PUBLIC },
    { name: "private", subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
  ],
});
```

### 4.2 Stack: Database (`database-stack.ts`)

```typescript
import * as rds from "aws-cdk-lib/aws-rds";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as secretsmanager from "aws-cdk-lib/aws-secretsmanager";

// Auto-generated credentials stored in Secrets Manager
const dbSecret = new secretsmanager.Secret(this, "DbSecret", {
  generateSecretString: {
    secretStringTemplate: JSON.stringify({ username: "lexiban" }),
    generateStringKey: "password",
    excludePunctuation: true,
  },
});

const db = new rds.DatabaseInstance(this, "Db", {
  engine: rds.DatabaseInstanceEngine.postgres({
    version: rds.PostgresEngineVersion.VER_17,
  }),
  instanceType: ec2.InstanceType.of(
    ec2.InstanceClass.T4G,
    ec2.InstanceSize.MICRO,
  ),
  vpc,
  vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
  credentials: rds.Credentials.fromSecret(dbSecret),
  databaseName: "lexiban",
  removalPolicy: RemovalPolicy.DESTROY, // Demo — delete DB on stack destroy
  deletionProtection: false,
});

// RDS Proxy — pools Lambda connections to avoid exhausting DB connections
const proxy = new rds.DatabaseProxy(this, "DbProxy", {
  proxyTarget: rds.ProxyTarget.fromInstance(db),
  secrets: [dbSecret],
  vpc,
  requireTLS: true,
});
```

### 4.3 Stack: Backend (`backend-stack.ts`)

```typescript
import * as lambda from "aws-cdk-lib/aws-lambda";
import { HttpApi, HttpMethod } from "aws-cdk-lib/aws-apigatewayv2";
import { HttpLambdaIntegration } from "aws-cdk-lib/aws-apigatewayv2-integrations";

const backendFn = new lambda.Function(this, "BackendFn", {
  runtime: lambda.Runtime.JAVA_21,
  handler: "de.nicograef.iban.StreamLambdaHandler",
  code: lambda.Code.fromAsset("../backend/target/lexiban-0.0.1-SNAPSHOT.jar"),
  memorySize: 1024,
  timeout: Duration.seconds(30),
  snapStart: lambda.SnapStartConf.ON_PUBLISHED_VERSIONS,
  vpc,
  vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
  environment: {
    DB_HOST: proxy.endpoint,
    DB_NAME: "lexiban",
    SPRING_PROFILES_ACTIVE: "aws",
  },
});

// Lambda reads DB credentials from Secrets Manager at runtime
dbSecret.grantRead(backendFn);
// Lambda connects to RDS via RDS Proxy
proxy.grantConnect(backendFn);

// HTTP API (cheaper and simpler than REST API)
const api = new HttpApi(this, "Api");
api.addRoutes({
  path: "/api/{proxy+}",
  methods: [HttpMethod.ANY],
  integration: new HttpLambdaIntegration("BackendIntegration", backendFn),
});
```

### 4.4 Stack: Frontend (`frontend-stack.ts`)

```typescript
import * as s3 from "aws-cdk-lib/aws-s3";
import * as cloudfront from "aws-cdk-lib/aws-cloudfront";
import * as origins from "aws-cdk-lib/aws-cloudfront-origins";
import * as s3deploy from "aws-cdk-lib/aws-s3-deployment";

const siteBucket = new s3.Bucket(this, "SiteBucket", {
  blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
  removalPolicy: RemovalPolicy.DESTROY,
  autoDeleteObjects: true,
});

const distribution = new cloudfront.Distribution(this, "Cdn", {
  defaultBehavior: {
    origin: origins.S3BucketOrigin.withOriginAccessControl(siteBucket),
    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
  },
  additionalBehaviors: {
    "/api/*": {
      origin: new origins.HttpOrigin(
        `${api.apiId}.execute-api.${this.region}.amazonaws.com`,
      ),
      allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
      cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
      originRequestPolicy:
        cloudfront.OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER,
    },
  },
  defaultRootObject: "index.html",
  errorResponses: [
    {
      httpStatus: 403,
      responseHttpStatus: 200,
      responsePagePath: "/index.html", // SPA client-side routing fallback
    },
  ],
});

new s3deploy.BucketDeployment(this, "DeploySite", {
  sources: [s3deploy.Source.asset("../frontend/dist")],
  destinationBucket: siteBucket,
  distribution,
  distributionPaths: ["/*"],
});
```

### 4.5 CDK App Entry Point (`bin/lexiban.ts`)

```typescript
import { App } from "aws-cdk-lib";
import { NetworkStack } from "../lib/network-stack";
import { DatabaseStack } from "../lib/database-stack";
import { BackendStack } from "../lib/backend-stack";
import { FrontendStack } from "../lib/frontend-stack";

const app = new App();
const env = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: "eu-central-1",
};
const stage = app.node.tryGetContext("stage") || "dev"; // cdk deploy -c stage=prod

const network = new NetworkStack(app, `${stage}-Network`, { env });
const database = new DatabaseStack(app, `${stage}-Database`, {
  env,
  vpc: network.vpc,
});
const backend = new BackendStack(app, `${stage}-Backend`, {
  env,
  vpc: network.vpc,
  dbProxy: database.proxy,
  dbSecret: database.secret,
});
new FrontendStack(app, `${stage}-Frontend`, { env, api: backend.api });
```

Deploy dev: `cdk deploy --all -c stage=dev`
Deploy prod: `cdk deploy --all -c stage=prod`

---

## 5. Backend Code Changes

Only **3 files** need to be added/changed. The frontend needs **zero changes**.

### 5.1 New Maven dependency (`pom.xml`)

```xml
<dependency>
  <groupId>com.amazonaws.serverless</groupId>
  <artifactId>aws-serverless-java-container-springboot3</artifactId>
  <version>2.1.2</version>
</dependency>
```

### 5.2 New Lambda handler class

**File:** `backend/src/main/java/de/nicograef/iban/StreamLambdaHandler.java`

```java
package de.nicograef.iban;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

    static {
        handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(IbanApplication.class);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        handler.proxyStream(input, output, context);
    }
}
```

### 5.3 New Spring profile (`application-aws.properties`)

**File:** `backend/src/main/resources/application-aws.properties`

```properties
# ── AWS profile ──
# Activate with: SPRING_PROFILES_ACTIVE=aws (set via Lambda environment variable)

# DB connection via RDS Proxy endpoint (injected from Secrets Manager)
spring.datasource.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Lambda doesn't benefit from connection pooling — RDS Proxy handles it
spring.datasource.hikari.maximum-pool-size=1
spring.datasource.hikari.connection-timeout=5000
```

---

## 6. GitHub Actions Deploy Workflow

**File:** `.github/workflows/deploy.yml`

Uses OIDC for AWS authentication — no static credentials stored.

```yaml
name: Deploy to AWS

on:
  push:
    branches: [main]

permissions:
  id-token: write
  contents: read

env:
  AWS_REGION: eu-central-1

jobs:
  # ── CI (reuse existing checks) ──
  backend-ci:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./backend
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - run: ./mvnw verify -B

  frontend-ci:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./frontend
    steps:
      - uses: actions/checkout@v5
      - uses: pnpm/action-setup@v4
        with: { version: 10, run_install: false }
      - uses: actions/setup-node@v6
        with:
          node-version: 24
          cache: pnpm
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: pnpm install --frozen-lockfile
      - run: pnpm run lint
      - run: pnpm run build
      - run: pnpm run test

  # ── Deploy (only after CI passes) ──
  deploy:
    needs: [backend-ci, frontend-ci]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5

      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      # Build backend JAR
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: maven }
      - run: cd backend && ./mvnw package -DskipTests -B

      # Build frontend
      - uses: pnpm/action-setup@v4
        with: { version: 10, run_install: false }
      - uses: actions/setup-node@v6
        with:
          node-version: 24
          cache: pnpm
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: cd frontend && pnpm install --frozen-lockfile && pnpm build

      # CDK deploy
      - run: cd infra && npm ci && npx cdk deploy --all -c stage=prod --require-approval never
```

---

## 7. Developer Setup Checklist

### A. AWS Account + CLI (~15 min)

```bash
# 1. Install AWS CLI
brew install awscli            # macOS
# or: sudo apt install awscli  # Linux

# 2. Configure credentials
aws configure                  # Enter Access Key ID + Secret + region eu-central-1

# 3. Install CDK + bootstrap
npm install -g aws-cdk
cdk bootstrap aws://$(aws sts get-caller-identity --query Account --output text)/eu-central-1
```

### B. GitHub → AWS OIDC Setup (~10 min)

1. **AWS Console → IAM → Identity Providers → Add Provider:**
   - Type: OpenID Connect
   - URL: `https://token.actions.githubusercontent.com`
   - Audience: `sts.amazonaws.com`

2. **Create IAM Role** `GitHubActionsDeployRole` with this trust policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
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
```

Attach `AdministratorAccess` policy (demo — scope down for production).

3. **GitHub → Repo → Settings → Secrets → New:**
   - Name: `AWS_ROLE_ARN`
   - Value: `arn:aws:iam::ACCOUNT_ID:role/GitHubActionsDeployRole`

### C. CDK Project Init (~15 min)

```bash
mkdir infra && cd infra
npx cdk init app --language typescript
npm install aws-cdk-lib constructs
```

Create the 4 stack files as described in section 4.

### D. Backend Lambda Adapter (~15 min)

1. Add Maven dependency (section 5.1).
2. Create `StreamLambdaHandler.java` (section 5.2).
3. Create `application-aws.properties` (section 5.3).

### E. First Deploy (~15 min)

```bash
cd infra
cdk synth                        # Generate CloudFormation (review)
cdk deploy --all -c stage=dev    # Deploy all stacks

# After deploy, CDK prints the CloudFront URL:
# ✅ dev-Frontend.CdnUrl = https://d1234abcd.cloudfront.net
```

### F. Verify

```bash
# Test API
curl -X POST https://d1234abcd.cloudfront.net/api/ibans \
  -H 'Content-Type: application/json' \
  -d '{"iban": "DE89370400440532013000"}'

# Open SPA
open https://d1234abcd.cloudfront.net
```

### G. Teardown (after demo)

```bash
cdk destroy --all -c stage=dev   # Deletes all AWS resources
```

---

## 8. Gotchas

### SnapStart + Stale DB Connections

SnapStart captures a JVM snapshot after Spring Boot init (including Flyway + HikariCP). On restore, those connections may be dead. Fix: set `maximum-pool-size=1` and short `connection-timeout` in `application-aws.properties` — RDS Proxy handles the actual pooling.

### CORS Not Needed

`CorsConfig` is `@Profile("dev")` only. On AWS, CloudFront serves SPA and `/api/*` from the same domain — same origin, no CORS needed.

### NAT Gateway Required

Lambda runs in a VPC (to reach RDS). The openiban.com external call needs internet → NAT Gateway is required. For a few days of demo this costs ~$1-2, irrelevant.

> **Future improvement:** To eliminate NAT in production, move the openiban.com call to a separate non-VPC Lambda, or drop the external API dependency entirely.

### Frontend: Zero Changes

The frontend uses `const API_BASE = '/api/ibans'` (relative path). CloudFront routes `/api/*` to API Gateway. No code changes needed.

### Flyway Runs on First Request

After deploy, the first Lambda invocation boots Spring Boot → Flyway runs migrations against the empty RDS. This first request is slow (~5-10s) but subsequent requests are fast.

### Lambda JAR Packaging

`spring-boot-maven-plugin` produces a fat JAR with embedded Tomcat. `aws-serverless-java-container-springboot3` doesn't change the build — it intercepts API Gateway events and routes them into DispatcherServlet. The existing `./mvnw package` produces the correct artifact.

---

## 9. Summary

| Aspect              | Detail                                                                                             |
| ------------------- | -------------------------------------------------------------------------------------------------- |
| **AWS Services**    | S3, CloudFront, Lambda, API Gateway (HTTP), RDS, RDS Proxy, Secrets Manager, VPC, NAT Gateway, IAM |
| **Code changes**    | 1 new Java class, 1 properties file, 1 Maven dependency                                            |
| **New infra files** | 4 CDK stacks + entry point, 1 GitHub Actions workflow                                              |
| **Frontend**        | Zero changes                                                                                       |
| **Time to build**   | ~2-3 hours (assuming AWS account exists)                                                           |
| **Teardown**        | `cdk destroy --all` deletes everything                                                             |
