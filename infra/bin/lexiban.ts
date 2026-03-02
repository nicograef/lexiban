#!/usr/bin/env node
import "source-map-support/register";
import { App, RemovalPolicy } from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as cloudfront from "aws-cdk-lib/aws-cloudfront";
import { NetworkStack } from "../lib/network-stack";
import { DatabaseStack } from "../lib/database-stack";
import { BackendStack } from "../lib/backend-stack";
import { FrontendStack } from "../lib/frontend-stack";

const app = new App();

const env = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: "eu-central-1",
};

const stage = app.node.tryGetContext("stage") || "dev";
const isProd = stage === "prod";

// ── Network ──
const network = new NetworkStack(app, `${stage}-Network`, {
  env,
  natGateways: isProd ? 2 : 1,
});

// ── Database ──
const database = new DatabaseStack(app, `${stage}-Database`, {
  env,
  vpc: network.vpc,
  instanceClass: ec2.InstanceClass.T4G,
  instanceSize: isProd ? ec2.InstanceSize.SMALL : ec2.InstanceSize.MICRO,
  deletionProtection: isProd,
  removalPolicy: isProd ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY,
});

// ── Backend (Lambda + API Gateway) ──
const backend = new BackendStack(app, `${stage}-Backend`, {
  env,
  vpc: network.vpc,
  dbProxy: database.proxy,
  dbSecret: database.secret,
  lambdaMemory: isProd ? 1024 : 512,
});

// ── Frontend (S3 + CloudFront) ──
new FrontendStack(app, `${stage}-Frontend`, {
  env,
  api: backend.api,
  priceClass: isProd
    ? cloudfront.PriceClass.PRICE_CLASS_200
    : cloudfront.PriceClass.PRICE_CLASS_100,
});
