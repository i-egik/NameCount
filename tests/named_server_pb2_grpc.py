# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
"""Client and server classes corresponding to protobuf-defined services."""
import grpc
import warnings

import named_server_pb2 as named__server__pb2

GRPC_GENERATED_VERSION = '1.73.0'
GRPC_VERSION = grpc.__version__
_version_not_supported = False

try:
    from grpc._utilities import first_version_is_lower
    _version_not_supported = first_version_is_lower(GRPC_VERSION, GRPC_GENERATED_VERSION)
except ImportError:
    _version_not_supported = True

if _version_not_supported:
    raise RuntimeError(
        f'The grpc package installed is at version {GRPC_VERSION},'
        + f' but the generated code in named_server_pb2_grpc.py depends on'
        + f' grpcio>={GRPC_GENERATED_VERSION}.'
        + f' Please upgrade your grpc module to grpcio>={GRPC_GENERATED_VERSION}'
        + f' or downgrade your generated code using grpcio-tools<={GRPC_VERSION}.'
    )


class CountServiceStub(object):
    """Missing associated documentation comment in .proto file."""

    def __init__(self, channel):
        """Constructor.

        Args:
            channel: A grpc.Channel.
        """
        self.Reset = channel.unary_unary(
                '/named.CountService/Reset',
                request_serializer=named__server__pb2.CountFilter.SerializeToString,
                response_deserializer=named__server__pb2.CountValue.FromString,
                _registered_method=True)
        self.Get = channel.unary_unary(
                '/named.CountService/Get',
                request_serializer=named__server__pb2.CountFilter.SerializeToString,
                response_deserializer=named__server__pb2.CountValue.FromString,
                _registered_method=True)
        self.List = channel.unary_stream(
                '/named.CountService/List',
                request_serializer=named__server__pb2.CountFilter.SerializeToString,
                response_deserializer=named__server__pb2.CountValue.FromString,
                _registered_method=True)
        self.Increment = channel.unary_unary(
                '/named.CountService/Increment',
                request_serializer=named__server__pb2.CountIncrementRequest.SerializeToString,
                response_deserializer=named__server__pb2.CountValue.FromString,
                _registered_method=True)


class CountServiceServicer(object):
    """Missing associated documentation comment in .proto file."""

    def Reset(self, request, context):
        """Missing associated documentation comment in .proto file."""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def Get(self, request, context):
        """Получение значение конкретного счетчика
        """
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def List(self, request, context):
        """Получение всех счетчиков пользователя
        """
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def Increment(self, request, context):
        """Запись значения
        TODO: Добавить установку конкретного значения
        TODO: Обнуление счетчика
        """
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_CountServiceServicer_to_server(servicer, server):
    rpc_method_handlers = {
            'Reset': grpc.unary_unary_rpc_method_handler(
                    servicer.Reset,
                    request_deserializer=named__server__pb2.CountFilter.FromString,
                    response_serializer=named__server__pb2.CountValue.SerializeToString,
            ),
            'Get': grpc.unary_unary_rpc_method_handler(
                    servicer.Get,
                    request_deserializer=named__server__pb2.CountFilter.FromString,
                    response_serializer=named__server__pb2.CountValue.SerializeToString,
            ),
            'List': grpc.unary_stream_rpc_method_handler(
                    servicer.List,
                    request_deserializer=named__server__pb2.CountFilter.FromString,
                    response_serializer=named__server__pb2.CountValue.SerializeToString,
            ),
            'Increment': grpc.unary_unary_rpc_method_handler(
                    servicer.Increment,
                    request_deserializer=named__server__pb2.CountIncrementRequest.FromString,
                    response_serializer=named__server__pb2.CountValue.SerializeToString,
            ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
            'named.CountService', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))
    server.add_registered_method_handlers('named.CountService', rpc_method_handlers)


 # This class is part of an EXPERIMENTAL API.
class CountService(object):
    """Missing associated documentation comment in .proto file."""

    @staticmethod
    def Reset(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/named.CountService/Reset',
            named__server__pb2.CountFilter.SerializeToString,
            named__server__pb2.CountValue.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)

    @staticmethod
    def Get(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/named.CountService/Get',
            named__server__pb2.CountFilter.SerializeToString,
            named__server__pb2.CountValue.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)

    @staticmethod
    def List(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_stream(
            request,
            target,
            '/named.CountService/List',
            named__server__pb2.CountFilter.SerializeToString,
            named__server__pb2.CountValue.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)

    @staticmethod
    def Increment(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/named.CountService/Increment',
            named__server__pb2.CountIncrementRequest.SerializeToString,
            named__server__pb2.CountValue.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)


class CatalogueServiceStub(object):
    """Missing associated documentation comment in .proto file."""

    def __init__(self, channel):
        """Constructor.

        Args:
            channel: A grpc.Channel.
        """
        self.List = channel.unary_unary(
                '/named.CatalogueService/List',
                request_serializer=named__server__pb2.CatalogueFilter.SerializeToString,
                response_deserializer=named__server__pb2.CatalogueReplyList.FromString,
                _registered_method=True)
        self.Put = channel.unary_unary(
                '/named.CatalogueService/Put',
                request_serializer=named__server__pb2.CataloguePutRequest.SerializeToString,
                response_deserializer=named__server__pb2.CatalogueReplyValue.FromString,
                _registered_method=True)
        self.Update = channel.unary_unary(
                '/named.CatalogueService/Update',
                request_serializer=named__server__pb2.CatalogueUpdateRequest.SerializeToString,
                response_deserializer=named__server__pb2.CatalogueReplyValue.FromString,
                _registered_method=True)


class CatalogueServiceServicer(object):
    """Missing associated documentation comment in .proto file."""

    def List(self, request, context):
        """Получение списка счетчиков
        """
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def Put(self, request, context):
        """Создание или обновление счетчика
        """
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def Update(self, request, context):
        """Missing associated documentation comment in .proto file."""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_CatalogueServiceServicer_to_server(servicer, server):
    rpc_method_handlers = {
            'List': grpc.unary_unary_rpc_method_handler(
                    servicer.List,
                    request_deserializer=named__server__pb2.CatalogueFilter.FromString,
                    response_serializer=named__server__pb2.CatalogueReplyList.SerializeToString,
            ),
            'Put': grpc.unary_unary_rpc_method_handler(
                    servicer.Put,
                    request_deserializer=named__server__pb2.CataloguePutRequest.FromString,
                    response_serializer=named__server__pb2.CatalogueReplyValue.SerializeToString,
            ),
            'Update': grpc.unary_unary_rpc_method_handler(
                    servicer.Update,
                    request_deserializer=named__server__pb2.CatalogueUpdateRequest.FromString,
                    response_serializer=named__server__pb2.CatalogueReplyValue.SerializeToString,
            ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
            'named.CatalogueService', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))
    server.add_registered_method_handlers('named.CatalogueService', rpc_method_handlers)


 # This class is part of an EXPERIMENTAL API.
class CatalogueService(object):
    """Missing associated documentation comment in .proto file."""

    @staticmethod
    def List(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/named.CatalogueService/List',
            named__server__pb2.CatalogueFilter.SerializeToString,
            named__server__pb2.CatalogueReplyList.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)

    @staticmethod
    def Put(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/named.CatalogueService/Put',
            named__server__pb2.CataloguePutRequest.SerializeToString,
            named__server__pb2.CatalogueReplyValue.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)

    @staticmethod
    def Update(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/named.CatalogueService/Update',
            named__server__pb2.CatalogueUpdateRequest.SerializeToString,
            named__server__pb2.CatalogueReplyValue.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)
