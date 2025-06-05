package ru.pastor.templates.named.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.pastor.templates.named.configuration.ApplicationConfiguration;
import ru.pastor.templates.named.configuration.CacheConfiguration;
import ru.pastor.templates.named.configuration.RedisConfiguration;

/**
 * Главный класс приложения, являющийся точкой входа для запуска сервера именованных счетчиков.
 * Настроен как Spring Boot приложение с отключением стандартных автоконфигураций
 * и импортом пользовательских конфигураций.
 */
@SpringBootApplication(exclude = {
  DataSourceAutoConfiguration.class,
  RedisReactiveAutoConfiguration.class,
  RedisAutoConfiguration.class,
  ValidationAutoConfiguration.class,
  R2dbcAutoConfiguration.class
})
@Import({
  ApplicationConfiguration.class,
  RedisConfiguration.class,
  CacheConfiguration.class
})
public class Main {
  /**
   * Точка входа в приложение.
   * Запускает Spring Boot приложение с указанными аргументами командной строки.
   *
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }
}
