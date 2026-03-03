#!/usr/bin/env node
import "source-map-support/register";
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

const stage = app.node.tryGetContext("stage") || "dev";
const isProd = stage === "prod";

const network = new NetworkStack(app, `${stage}-Network`, {
  env,
});

const database = new DatabaseStack(app, `${stage}-Database`, {
  env,
  vpc: network.vpc,
  isProd,
});

const backend = new BackendStack(app, `${stage}-Backend`, {
  env,
  vpc: network.vpc,
  dbProxy: database.proxy,
  dbSecret: database.secret,
});

new FrontendStack(app, `${stage}-Frontend`, {
  env,
  api: backend.api,
});
