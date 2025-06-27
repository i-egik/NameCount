
# Генерация
```shell
export PROTO_DIR=~/github/named-count/middleware/named-server
python -m grpc_tools.protoc -I${PROTO_DIR} --python_out=. --grpc_python_out=. ${PROTO_DIR}/named-server.proto
```
