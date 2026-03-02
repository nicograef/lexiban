import { Duration, RemovalPolicy, Stack, StackProps } from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as rds from "aws-cdk-lib/aws-rds";
import * as secretsmanager from "aws-cdk-lib/aws-secretsmanager";
import { Construct } from "constructs";

export interface DatabaseStackProps extends StackProps {
  vpc: ec2.IVpc;
  /** RDS instance class — e.g. ec2.InstanceClass.T4G */
  instanceClass?: ec2.InstanceClass;
  /** RDS instance size — e.g. ec2.InstanceSize.MICRO */
  instanceSize?: ec2.InstanceSize;
  /** Whether to protect the DB from deletion (prod = true). */
  deletionProtection?: boolean;
  /** CDK removal policy (DESTROY for dev, RETAIN for prod). */
  removalPolicy?: RemovalPolicy;
}

/**
 * RDS PostgreSQL 17 + RDS Proxy + Secrets Manager.
 * RDS Proxy pools Lambda connections to avoid exhausting DB connections.
 */
export class DatabaseStack extends Stack {
  public readonly proxy: rds.DatabaseProxy;
  public readonly secret: secretsmanager.ISecret;

  constructor(scope: Construct, id: string, props: DatabaseStackProps) {
    super(scope, id, props);

    const { vpc } = props;
    const removalPolicy = props.removalPolicy ?? RemovalPolicy.DESTROY;
    const deletionProtection = props.deletionProtection ?? false;
    const instanceClass = props.instanceClass ?? ec2.InstanceClass.T4G;
    const instanceSize = props.instanceSize ?? ec2.InstanceSize.MICRO;

    // Auto-generated credentials stored in Secrets Manager
    const dbSecret = new secretsmanager.Secret(this, "DbSecret", {
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: "lexiban" }),
        generateStringKey: "password",
        excludePunctuation: true,
      },
    });
    this.secret = dbSecret;

    // Security group for the RDS instance
    const dbSg = new ec2.SecurityGroup(this, "DbSg", {
      vpc,
      description: "Allow inbound PostgreSQL from VPC",
      allowAllOutbound: false,
    });
    dbSg.addIngressRule(
      ec2.Peer.ipv4(vpc.vpcCidrBlock),
      ec2.Port.tcp(5432),
      "PostgreSQL from VPC",
    );

    const db = new rds.DatabaseInstance(this, "Db", {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_17,
      }),
      instanceType: ec2.InstanceType.of(instanceClass, instanceSize),
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [dbSg],
      credentials: rds.Credentials.fromSecret(dbSecret),
      databaseName: "lexiban",
      removalPolicy,
      deletionProtection,
      backupRetention: Duration.days(deletionProtection ? 7 : 0),
      storageEncrypted: true,
    });

    // RDS Proxy — pools Lambda connections to avoid exhausting DB connections
    this.proxy = new rds.DatabaseProxy(this, "DbProxy", {
      proxyTarget: rds.ProxyTarget.fromInstance(db),
      secrets: [dbSecret],
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [dbSg],
      requireTLS: true,
    });
  }
}
