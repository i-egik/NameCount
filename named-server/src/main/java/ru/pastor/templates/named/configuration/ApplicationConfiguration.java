package ru.pastor.templates.named.configuration;

import io.grpc.BindableService;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import ru.pastor.templates.named.cache.NamedCache;
import ru.pastor.templates.named.service.NamedCountService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
@Import({DatabaseConfiguration.class, RedisConfiguration.class, CacheConfiguration.class})
@ComponentScan({
  "ru.pastor.templates.named.service",
  "ru.pastor.templates.named.repository"
})
public class ApplicationConfiguration {

  @Bean
  public NamedCountService namedCountService(
    @Qualifier("NamedCache.Values") NamedCache<String, Long> values,
    @Qualifier("NamedCache.Catalogue") NamedCache<String, Long> catalogue) {
    return new NamedCountService.Standard(values, catalogue);
  }

  @Bean
  public ServerInterceptor loggingInterceptor() {
    return new LogInterceptor();
  }

  @Bean
  public GrpcServer grpcServer(final List<BindableService> services) {
    return new GrpcServer(30323, services, List.of(), 5, 30, false);
  }

  @Slf4j
  public static class GrpcServer {
    private final Server server;

    private GrpcServer(int port,
                       List<BindableService> services,
                       List<ServerInterceptor> serverInterceptors,
                       long keepAliveTime,
                       long keepAliveTimeout,
                       boolean keepAliveWithoutCalls) {
      ServerBuilder<? extends ServerBuilder<?>> builder = ServerBuilder.forPort(port);
      Objects.requireNonNull(builder);
      serverInterceptors.forEach(builder::intercept);
      Objects.requireNonNull(builder);
      services.forEach(builder::addService);
      builder.keepAliveTime(keepAliveTime, TimeUnit.MINUTES)
        .keepAliveTimeout(keepAliveTimeout, TimeUnit.SECONDS)
        .permitKeepAliveWithoutCalls(keepAliveWithoutCalls);
      this.server = builder.build();
    }

    @EventListener(ApplicationStartedEvent.class)
    public void ready() throws IOException {
      this.server.start();
      if (log.isInfoEnabled()) {
        log.info("Server started, listening on {}", this.server.getPort());
      }
    }

    @EventListener(ApplicationFailedEvent.class)
    public void failed() {
      this.server.shutdown();
    }
  }

  @Slf4j
  private static final class LogInterceptor implements ServerInterceptor {
    @Override
    public <I, O> ServerCall.Listener<I> interceptCall(ServerCall<I, O> serverCall, Metadata metadata,
                                                       ServerCallHandler<I, O> next) {
      logMessage(serverCall);
      ServerCall.Listener<I> delegate = next.startCall(serverCall, metadata);
      return delegate;
    }

    private <I, O> void logMessage(ServerCall<I, O> call) {
      log.debug("call: {}", call.getMethodDescriptor().getFullMethodName());
    }
  }
}
