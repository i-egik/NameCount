package ru.pastor.templates.named.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

public interface CounterRepository {

  @Slf4j
  @RequiredArgsConstructor
  @Service("CounterRepository.Postgres")
  class Postgres implements CounterRepository {
    private final DatabaseClient client;
    private final TransactionalOperator tx;
  }
}
