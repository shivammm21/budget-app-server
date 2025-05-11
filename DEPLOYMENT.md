# Budget App Server Deployment Guide

This guide details how to deploy the Budget App Server to a vendor platform using environment variables for configuration.

## Environment Variables

Configure the following environment variables in your vendor platform:

### Required Environment Variables

| Variable Name | Description | Example Value |
|---------------|-------------|--------------|
| `JDBC_DATABASE_URL` | Database connection URL | `jdbc:mysql://your-db-host:3306/budget?useSSL=true` |
| `JDBC_DATABASE_USERNAME` | Database username | `dbuser` |
| `JDBC_DATABASE_PASSWORD` | Database password | `password123` |
| `ENCRYPTION_KEY` | Encryption key for sensitive data (must be exactly 16 characters) | `4h8rL2mN7vXqPzE3` |

### Optional Environment Variables

| Variable Name | Description | Default Value |
|---------------|-------------|--------------|
| `PORT` | Server port | `8080` |
| `APP_NAME` | Application name | `budget-app` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `JPA_HIBERNATE_DDL_AUTO` | Hibernate DDL auto strategy | `update` |
| `SHOW_SQL` | Show SQL in logs | `false` |
| `FLYWAY_ENABLED` | Enable Flyway migrations | `true` |
| `FLYWAY_BASELINE_ON_MIGRATE` | Baseline on Flyway migrate | `true` |
| `FLYWAY_LOCATIONS` | Flyway migration locations | `classpath:db/migration` |
| `LOG_LEVEL_SPRING` | Spring logging level | `INFO` |
| `LOG_LEVEL_APP` | Application logging level | `INFO` |
| `LOG_LEVEL_HIBERNATE_SQL` | Hibernate SQL logging level | `INFO` |
| `CORS_ALLOWED_ORIGINS` | CORS allowed origins | `*` |
| `DB_MAX_POOL_SIZE` | Maximum database connection pool size | `10` |
| `DB_MIN_IDLE` | Minimum idle database connections | `5` |
| `DB_CONNECTION_TIMEOUT` | Database connection timeout (ms) | `30000` |
| `TOMCAT_MAX_THREADS` | Maximum Tomcat threads | `200` |
| `TOMCAT_MIN_SPARE_THREADS` | Minimum spare Tomcat threads | `10` |

### Google Cloud SQL Configuration (If Needed)

| Variable Name | Description | Default Value |
|---------------|-------------|--------------|
| `GCP_SQL_ENABLED` | Enable Google Cloud SQL | `false` |
| `GCP_SQL_INSTANCE_CONNECTION_NAME` | GCP SQL instance connection name | `` |
| `GCP_SQL_DATABASE_NAME` | GCP SQL database name | `budget` |

## Deployment Steps

1. Build the application:
   ```
   ./mvnw clean package -DskipTests
   ```

2. Configure environment variables in your vendor platform dashboard.

3. For production deployment, set:
   ```
   SPRING_PROFILES_ACTIVE=prod
   ```

4. Deploy the JAR file to your vendor platform.

5. Verify the application is running by checking logs.

## Profile-Specific Configuration

The application includes configuration profiles:

- `dev`: Development environment with local database and more verbose logging
- `prod`: Production environment with optimized settings for performance and security

Select the appropriate profile using `SPRING_PROFILES_ACTIVE` environment variable.

## Important Security Notes

1. Never commit real database credentials or encryption keys to source control.
2. Change the default encryption key (`ENCRYPTION_KEY`) in production.
3. In production, set `JPA_HIBERNATE_DDL_AUTO` to `none` or `validate` to protect your database schema.

## Troubleshooting

- **Database Connection Issues**: Verify `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, and `JDBC_DATABASE_PASSWORD` are correct.
- **Encryption/Decryption Errors**: Ensure `ENCRYPTION_KEY` is exactly 16 characters long.
- **Startup Failures**: Check application logs for details. 