package de.nicograef.lexiban;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import org.flywaydb.core.Flyway;

/**
 * Lightweight Lambda handler that runs Flyway migrations against the database. Triggered by a CDK
 * Custom Resource during each deployment — before the app Lambda starts serving traffic.
 *
 * <p>No Spring Boot — just plain Flyway CLI. Keeps cold start under 5 seconds.
 */
public class FlywayMigrationHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        String host = System.getenv("DB_HOST");
        String dbName = System.getenv("DB_NAME");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        String url = "jdbc:postgresql://" + host + ":5432/" + dbName + "?sslmode=require";

        context.getLogger().log("Running Flyway migration against " + host + "/" + dbName);

        var result =
                Flyway.configure()
                        .dataSource(url, username, password)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();

        String message =
                "Flyway migration complete: "
                        + result.migrationsExecuted
                        + " migration(s) executed, schema version: "
                        + result.targetSchemaVersion;
        context.getLogger().log(message);

        return message;
    }
}
