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
  DataSourceAutoConfiguration.class,  // Отключаем автоконфигурацию JDBC, так как используем R2DBC
  RedisReactiveAutoConfiguration.class,  // Отключаем стандартную автоконфигурацию Redis, используем свою
  RedisAutoConfiguration.class,  // Отключаем стандартную автоконфигурацию Redis, используем свою
  ValidationAutoConfiguration.class,  // Отключаем стандартную валидацию, используем свою
  R2dbcAutoConfiguration.class  // Отключаем стандартную автоконфигурацию R2DBC, используем свою
})
@Import({
  ApplicationConfiguration.class,  // Импортируем основную конфигурацию приложения
  RedisConfiguration.class,  // Импортируем конфигурацию Redis
  CacheConfiguration.class  // Импортируем конфигурацию кэша
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
