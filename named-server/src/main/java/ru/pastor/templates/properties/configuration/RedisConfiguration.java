package ru.pastor.templates.properties.configuration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfiguration {
  private static final long COMMAND_TIMEOUT = 2000;

  @Bean
  @Primary
  public ReactiveRedisConnectionFactory redisConnectionFactory(@Value("${spring.data.redis.host}") String cacheHost,
                                                               @Value("${spring.data.redis.port:6379}") int cachePort,
                                                               @Value("${spring.data.redis.db:14}") int cacheDatabase) {
    var lettuceClientConfiguration = LettuceClientConfiguration.builder()
      .clientOptions(ClientOptions.builder().autoReconnect(true).build())
      .commandTimeout(Duration.ofMillis(COMMAND_TIMEOUT))
      .clientResources(ClientResources.builder().build())
      .build();
    var conf = new RedisStandaloneConfiguration(cacheHost, cachePort);
    conf.setDatabase(cacheDatabase);
    return new LettuceConnectionFactory(conf, lettuceClientConfiguration);
  }

  @Bean
  public ReactiveRedisOperations<String, String> redisOperations(ReactiveRedisConnectionFactory factory) {
    return new ReactiveStringRedisTemplate(factory);
  }
}
