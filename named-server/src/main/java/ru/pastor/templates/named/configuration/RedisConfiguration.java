package ru.pastor.templates.named.configuration;

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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.math.BigInteger;
import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfiguration {
  private static final long COMMAND_TIMEOUT = 2000;

  @Bean
  @Primary
  public ReactiveRedisConnectionFactory redisConnectionFactory(@Value("${spring.data.redis.host}") String cacheHost,
                                                               @Value("${spring.data.redis.port:6379}") int cachePort,
                                                               @Value("${spring.data.redis.db:13}") int cacheDatabase) {
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
  public ReactiveRedisMessageListenerContainer listenerContainer(ReactiveRedisConnectionFactory factory) {
    return new ReactiveRedisMessageListenerContainer(factory);
  }

  @Bean
  public ReactiveRedisOperations<String, String> textRedisOperations(ReactiveRedisConnectionFactory factory) {
    return new ReactiveStringRedisTemplate(factory);
  }

  @Bean
  public ReactiveRedisTemplate<String, Integer> longRedisOperations(ReactiveRedisConnectionFactory factory) {
    return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.<String, Integer>newSerializationContext()
      .key(RedisSerializer.string())
      .value(RedisSerializationContext.SerializationPair.fromSerializer(IntegerSerializer.INSTANCE))
      .hashKey(RedisSerializer.string())
      .hashValue(RedisSerializationContext.SerializationPair.fromSerializer(IntegerSerializer.INSTANCE))
      .build());
  }

  private static final class IntegerSerializer implements RedisSerializer<Integer> {
    private static final IntegerSerializer INSTANCE = new IntegerSerializer();

    @Override
    public byte[] serialize(Integer value) throws SerializationException {
      if (value == null) {
        return null;
      }
      return BigInteger.valueOf(value).toByteArray();
    }

    @Override
    public Integer deserialize(byte[] bytes) throws SerializationException {
      return new BigInteger(bytes).intValue();
    }
  }
}
