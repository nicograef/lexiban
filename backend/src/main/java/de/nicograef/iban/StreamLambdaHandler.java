package de.nicograef.iban;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AWS Lambda entry point — bridges API Gateway HTTP API v2 events into Spring Boot's
 * DispatcherServlet via aws-serverless-java-container.
 *
 * <p>The static initializer boots Spring Boot once (captured by SnapStart). Each invocation proxies
 * the Lambda event stream into the running Spring context.
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse>
            HANDLER;

    static {
        try {
            HANDLER =
                    SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(
                            IbanApplication.class);
        } catch (ContainerInitializationException e) {
            throw new IllegalStateException("Failed to initialize Spring Boot Lambda handler", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        HANDLER.proxyStream(input, output, context);
    }
}
