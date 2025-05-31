package ru.pastor.templates.named.service;

import org.springframework.stereotype.Service;
import ru.pastor.templates.named.server.grpc.ReactorCountServiceGrpc;

@Service("NamedServerService.Count.Grpc")
public class NamedServerCountServiceGrpc extends ReactorCountServiceGrpc.CountServiceImplBase {

}
