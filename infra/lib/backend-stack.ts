import { Duration, Stack, StackProps } from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as rds from "aws-cdk-lib/aws-rds";
import * as secretsmanager from "aws-cdk-lib/aws-secretsmanager";
import { HttpApi, HttpMethod } from "aws-cdk-lib/aws-apigatewayv2";
import { HttpLambdaIntegration } from "aws-cdk-lib/aws-apigatewayv2-integrations";
import { Construct } from "constructs";

export interface BackendStackProps extends StackProps {
  vpc: ec2.IVpc;
  dbProxy: rds.DatabaseProxy;
  dbSecret: secretsmanager.ISecret;
}

/**
 * Lambda (Spring Boot + SnapStart) behind an HTTP API Gateway.
 * Connects to RDS via RDS Proxy.
 */
export class BackendStack extends Stack {
  public readonly api: HttpApi;

  constructor(scope: Construct, id: string, props: BackendStackProps) {
    super(scope, id, props);

    const { vpc, dbProxy, dbSecret } = props;

    // Lambda function running Spring Boot via aws-serverless-java-container
    const backend = new lambda.Function(this, "Backend", {
      runtime: lambda.Runtime.JAVA_21,
      handler: "de.nicograef.lexiban.StreamLambdaHandler",
      code: lambda.Code.fromAsset(
        "../backend/target/lexiban-0.0.1-SNAPSHOT.jar",
      ),
      memorySize: 512,
      timeout: Duration.seconds(30),
      snapStart: lambda.SnapStartConf.ON_PUBLISHED_VERSIONS,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      environment: {
        DB_HOST: dbProxy.endpoint,
        DB_NAME: "lexiban",
        DB_USERNAME: dbSecret.secretValueFromJson("username").unsafeUnwrap(),
        DB_PASSWORD: dbSecret.secretValueFromJson("password").unsafeUnwrap(),
        SPRING_PROFILES_ACTIVE: "aws",
      },
    });

    // Lambda reads DB credentials from Secrets Manager at runtime
    dbSecret.grantRead(backend);
    // Lambda connects to RDS via RDS Proxy
    dbProxy.grantConnect(backend);
    // Allow Lambda → RDS Proxy on port 5432 (grantConnect only sets IAM policy)
    dbProxy.connections.allowDefaultPortFrom(backend);

    // Publish a version (required for SnapStart + Provisioned Concurrency)
    const version = backend.currentVersion;

    // Alias with provisioned concurrency — keeps 1 instance always warm (no cold starts)
    const alias = new lambda.Alias(this, "BackendAlias", {
      aliasName: "live",
      version,
      provisionedConcurrentExecutions: 1,
    });

    // HTTP API (cheaper and simpler than REST API for proxying to Lambda)
    this.api = new HttpApi(this, "Api");
    this.api.addRoutes({
      path: "/api/{proxy+}",
      methods: [HttpMethod.ANY],
      integration: new HttpLambdaIntegration("BackendIntegration", alias),
    });
  }
}
