
# Генерация
```shell
export PROTO_DIR=~/github/named-count/middleware/named-server
mkdir .gen
python -m grpc_tools.protoc -I${PROTO_DIR} --python_out=.gen --grpc_python_out=.gen ${PROTO_DIR}/named-server.proto
```
