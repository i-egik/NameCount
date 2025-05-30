package ru.pastor.templates.properties.configuration;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class DatabaseConfiguration {
  @Bean
  @ConfigurationProperties("spring.r2dbc")
  public R2dbcProperties properties() {
    return new R2dbcProperties();
  }

  @Bean
  public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }

  @Bean
  public DatabaseClient databaseClient(ConnectionFactory factory) {
    return DatabaseClient.builder().connectionFactory(factory).build();
  }

  @Bean
  public TransactionalOperator transactionalOperator(ReactiveTransactionManager txManager) {
    return TransactionalOperator.create(txManager);
  }

  private ConnectionFactory getConnectionFactory(R2dbcProperties properties, String name) {
    var dbUrl = ConnectionFactoryOptions.parse(properties.getUrl());
    var host = (String) dbUrl.getRequiredValue(Option.valueOf("host"));
    var port = (int) dbUrl.getRequiredValue(Option.valueOf("port"));
    var database = (String) dbUrl.getRequiredValue(Option.valueOf("database"));
    var driver = (String) dbUrl.getRequiredValue(Option.valueOf("driver"));
    var connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
      .option(ConnectionFactoryOptions.DRIVER, driver)
      .option(ConnectionFactoryOptions.HOST, host)
      .option(ConnectionFactoryOptions.PORT, port)
      .option(ConnectionFactoryOptions.DATABASE, database)
      .option(ConnectionFactoryOptions.USER, properties.getUsername())
      .option(ConnectionFactoryOptions.PASSWORD, properties.getPassword())
      .option(Option.sensitiveValueOf("ApplicationName"), name)
      .build());
    ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(connectionFactory)
      .initialSize(properties.getPool().getInitialSize())
      .maxSize(properties.getPool().getMaxSize())
      .build();
    return new ConnectionPool(configuration);
  }

  @Bean
  public ConnectionFactory connectionFactory(@Value("${spring.application.name:properties-service}") String name) {
    return getConnectionFactory(properties(), name);
  }

}
