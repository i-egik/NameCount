package ru.pastor.templates.named.configuration;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableConfigurationProperties(R2dbcProperties.class)
public class DatabaseConfiguration {

  @Bean
  @ConditionalOnMissingBean(ReactiveTransactionManager.class)
  public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }

  @Bean
  @ConditionalOnMissingBean(DatabaseClient.class)
  public DatabaseClient databaseClient(ConnectionFactory factory) {
    return DatabaseClient.builder().connectionFactory(factory).build();
  }

  @Bean
  @ConditionalOnMissingBean(TransactionalOperator.class)
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
  @ConditionalOnMissingBean(ConnectionFactory.class)
  public ConnectionFactory connectionFactory(R2dbcProperties properties,
                                             @Value("${spring.application.name:named-count}") String name) {
    return getConnectionFactory(properties, name);
  }
}
