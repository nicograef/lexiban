import { RemovalPolicy, Stack, StackProps } from "aws-cdk-lib";
import * as cloudfront from "aws-cdk-lib/aws-cloudfront";
import * as origins from "aws-cdk-lib/aws-cloudfront-origins";
import * as s3 from "aws-cdk-lib/aws-s3";
import * as s3deploy from "aws-cdk-lib/aws-s3-deployment";
import { HttpApi } from "aws-cdk-lib/aws-apigatewayv2";
import { CfnOutput } from "aws-cdk-lib";
import { Construct } from "constructs";

export interface FrontendStackProps extends StackProps {
  api: HttpApi;
  /** CloudFront price class (PRICE_CLASS_100 for dev, PRICE_CLASS_200 for prod). */
  priceClass?: cloudfront.PriceClass;
}

/**
 * S3 bucket + CloudFront distribution serving the React SPA.
 * Routes /api/* to the API Gateway backend.
 */
export class FrontendStack extends Stack {
  constructor(scope: Construct, id: string, props: FrontendStackProps) {
    super(scope, id, props);

    const { api } = props;
    const priceClass =
      props.priceClass ?? cloudfront.PriceClass.PRICE_CLASS_100;

    // S3 bucket for static SPA assets (private — served via CloudFront OAC)
    const siteBucket = new s3.Bucket(this, "SiteBucket", {
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      removalPolicy: RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    // CloudFront distribution: default → S3, /api/* → API Gateway
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
      priceClass,
      errorResponses: [
        {
          httpStatus: 403,
          responseHttpStatus: 200,
          responsePagePath: "/index.html", // SPA client-side routing fallback
        },
      ],
    });

    // Deploy Vite build output to S3 and invalidate CloudFront cache
    new s3deploy.BucketDeployment(this, "DeploySite", {
      sources: [s3deploy.Source.asset("../frontend/dist")],
      destinationBucket: siteBucket,
      distribution,
      distributionPaths: ["/*"],
    });

    // Output the CloudFront URL
    new CfnOutput(this, "CdnUrl", {
      value: `https://${distribution.distributionDomainName}`,
      description: "CloudFront distribution URL",
    });
  }
}
