import grpc as ch

import named_server_pb2 as model
import named_server_pb2_grpc as grpc


def channel_sequence(channel):
  count_stub = grpc.CountServiceStub(channel)
  catalogue_stub = grpc.CatalogueServiceStub(channel)

  print("--- Создаем новый счетчик в каталоге ---")
  create_response = catalogue_stub.Put(
    model.CataloguePutRequest(
      name="requests_counter",
      description="Счетчик HTTP запросов"
    )
  )

  if create_response.status == model.Status.SUCCESS:
    counter_id = create_response.value.id
    print(f"Создан счетчик ID: {counter_id}")
  else:
    print(f"Ошибка создания: {create_response.error.message}")
    return

  print("\n--- Инкремент счетчика ---")
  increment_response = count_stub.Increment(
    model.CountIncrementRequest(
      user_id=123,
      id=counter_id,
      delta=5
    )
  )

  if increment_response.status == model.Status.SUCCESS:
    print(f"Новое значение: {increment_response.value}")
  else:
    print(f"Ошибка инкремента: {increment_response.error.message}")

  print("\n--- Получаем значение счетчика ---")
  get_response = count_stub.Get(
    model.CountFilter(
      user_id=123,
      id=counter_id
    )
  )

  if get_response.status == model.Status.SUCCESS:
    print(f"Текущее значение: {get_response.value}")
  else:
    print(f"Ошибка получения: {get_response.error.message}")

  print("\n--- Список счетчиков пользователя ---")
  list_response = count_stub.List(
    model.CountFilter(user_id=123)
  )

  for counter in list_response:
    if counter.status == model.Status.SUCCESS:
      print(f"ID: {counter.id}, Значение: {counter.value}")
    else:
      print(f"Ошибка элемента: {counter.error.message}")


def run_sync_client():
  with ch.insecure_channel('localhost:30323') as channel:
    channel_sequence(channel)


if __name__ == '__main__':
  run_sync_client()
