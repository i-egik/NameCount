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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import ru.pastor.templates.named.cache.NamedCache;
import ru.pastor.templates.named.repository.CatalogueRepository;
import ru.pastor.templates.named.service.NamedCatalogueService;
import ru.pastor.templates.named.service.NamedCountNotification;
import ru.pastor.templates.named.service.NamedCountService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
@Import(DatabaseConfiguration.class)
@ComponentScan({
  "ru.pastor.templates.named.service",
  "ru.pastor.templates.named.repository"
})
public class ApplicationConfiguration {

  @Bean
  public NamedCountService namedCountService(
    @Qualifier("NamedCache.Values") NamedCache<String, Integer> values,
    @Qualifier("NamedCache.Catalogue") NamedCache<String, Integer> catalogue,
    NamedCountNotification notification) {
    return new NamedCountService.Standard(values, catalogue, notification);
  }

  @Bean
  public NamedCatalogueService namedCatalogueService(
    @Qualifier("NamedCache.Catalogue") NamedCache<String, Integer> catalogue,
    CatalogueRepository catalogueRepository) {
    return new NamedCatalogueService.Standard(catalogue, catalogueRepository);
  }

  @Bean
  public ServerInterceptor loggingInterceptor() {
    return new LogInterceptor();
  }

  @Bean
  public GrpcServer grpcServer(@Value("${app.port:30323}") int port,
                               final List<BindableService> services,
                               final List<ServerInterceptor> interceptors) {
    return new GrpcServer(port, services, interceptors, 5, 30, false);
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
        log.info("GRPC started on port {}", this.server.getPort());
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
      return next.startCall(serverCall, metadata);
    }

    private <I, O> void logMessage(ServerCall<I, O> call) {
      log.debug("call: {}", call.getMethodDescriptor().getFullMethodName());
    }
  }
}
