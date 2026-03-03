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
  /** Lambda memory in MB (512 for dev, 1024 for prod). */
  lambdaMemory?: number;
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
    const lambdaMemory = props.lambdaMemory ?? 1024;

    // Lambda function running Spring Boot via aws-serverless-java-container
    const backendFn = new lambda.Function(this, "BackendFn", {
      runtime: lambda.Runtime.JAVA_21,
      handler: "de.nicograef.lexiban.StreamLambdaHandler",
      code: lambda.Code.fromAsset(
        "../backend/target/lexiban-0.0.1-SNAPSHOT.jar",
      ),
      memorySize: lambdaMemory,
      timeout: Duration.seconds(30),
      snapStart: lambda.SnapStartConf.ON_PUBLISHED_VERSIONS,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      environment: {
        DB_HOST: dbProxy.endpoint,
        DB_NAME: "lexiban",
        SPRING_PROFILES_ACTIVE: "aws",
      },
    });

    // Lambda reads DB credentials from Secrets Manager at runtime
    dbSecret.grantRead(backendFn);
    // Lambda connects to RDS via RDS Proxy
    dbProxy.grantConnect(backendFn);

    // HTTP API (cheaper and simpler than REST API for proxying to Lambda)
    this.api = new HttpApi(this, "Api");
    this.api.addRoutes({
      path: "/api/{proxy+}",
      methods: [HttpMethod.ANY],
      integration: new HttpLambdaIntegration("BackendIntegration", backendFn),
    });
  }
}
