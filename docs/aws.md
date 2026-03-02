# AWS Cloud-Native Deployment — Zusammenfassung

> **Status: Umgesetzt.** Docker Compose → AWS managed services via CDK.
> Siehe `infra/` für CDK-Stacks, `StreamLambdaHandler.java` für Lambda-Adapter.

---

## Service-Mapping

| Docker Compose | AWS |
|---|---|
| Nginx + React SPA | S3 + CloudFront |
| Spring Boot Container | Lambda + API Gateway (HTTP) + SnapStart |
| PostgreSQL Container | RDS PostgreSQL + RDS Proxy |
| Nginx Reverse Proxy | CloudFront (HTTPS) |
| docker-compose.yml | AWS CDK (TypeScript) |

## Architecture

```
CloudFront → /*       → S3 (SPA)
           → /api/*   → API Gateway → Lambda (Spring Boot) → RDS Proxy → RDS PostgreSQL
```

All backend resources in VPC with private subnets + NAT Gateway.

## CDK Stacks (`infra/`)

| Stack | Resources |
|---|---|
| `network-stack.ts` | VPC, subnets, NAT Gateway |
| `database-stack.ts` | RDS PostgreSQL, RDS Proxy, Secrets Manager |
| `backend-stack.ts` | Lambda (Java 21, SnapStart), API Gateway HTTP |
| `frontend-stack.ts` | S3 bucket, CloudFront distribution |

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
