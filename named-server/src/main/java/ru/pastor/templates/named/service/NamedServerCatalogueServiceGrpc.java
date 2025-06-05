package ru.pastor.templates.named.service;

import org.springframework.stereotype.Service;
import ru.pastor.templates.named.server.grpc.ReactorCountServiceGrpc;

/**
 * gRPC сервис для работы с каталогом счетчиков.
 * Предоставляет API для доступа к каталогу счетчиков через gRPC протокол.
 * В текущей версии класс является заглушкой и не содержит реализации методов.
 * Расширяет базовый класс ReactorCountServiceGrpc.CountServiceImplBase для реактивной обработки gRPC запросов.
 */
@Service("NamedServerService.Count.Grpc")
public class NamedServerCatalogueServiceGrpc extends ReactorCountServiceGrpc.CountServiceImplBase {

}
