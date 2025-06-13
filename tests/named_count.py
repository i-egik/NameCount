import grpc as ch
import psycopg2
from psycopg2.extras import DictCursor

import named_server_pb2 as model
import named_server_pb2_grpc as grpc
import utilities

logger = utilities.create_logger("named-count")

DB_CONFIG = {
  "dbname": "named_count",
  "user": "postgres",
  "password": "postgres",
  "host": "localhost",
  "port": "25432"
}


class PgNamedCount:
  def __init__(self, config=None):
    if config is None:
      config = DB_CONFIG
    self.config = config
    self.connection = None
    self.cursor = None

  def __enter__(self):
    self.connect()
    return self

  def __exit__(self, exc_type, exc_val, exc_tb):
    self.close()

  def connect(self):
    try:
      self.connection = psycopg2.connect(**self.config)
      self.cursor = self.connection.cursor(cursor_factory=DictCursor)
    except psycopg2.Error as e:
      logger.error(f"Ошибка подключения: {e}")
      raise

  def close(self):
    if self.cursor:
      self.cursor.close()
    if self.connection:
      self.connection.close()

  def get(self, name: str, user_id: int = 123):
    try:
      self.cursor.execute("SELECT value FROM named.counter_values cv "
                          "JOIN named.counter_catalogue cc ON cc.id = cv.counter_id "
                          "WHERE cc.name = %s AND cv.user_id = %s", (name, user_id))
      row = self.cursor.fetchone()
      if row is None:
        return False, -1
      return True, int(row[0])
    except psycopg2.Error as e:
      self.connection.rollback()
      logger.error(f"Ошибка выполнения запроса: {e}")
      return False, -1

  def create_count(self, name: str, description: str = "тестовый"):
    try:
      self.cursor.execute("INSERT INTO named.counter_catalogue (name, description) VALUES (%s, %s) "
                          "ON CONFLICT(name) DO UPDATE SET updated = CURRENT_TIMESTAMP RETURNING id", (name, description))
      return True, self.cursor.fetchone()[0]
    except psycopg2.Error as e:
      self.connection.rollback()
      logger.error(f"Ошибка выполнения запроса: {e}")
      return False

  def increment(self, name: str, user_id: int = 123, delta: int = 1):
    pass


class NamedCount:
  def __init__(self, address: str = 'localhost:30323'):
    self.channel = ch.insecure_channel(address)
    self.count = grpc.CountServiceStub(self.channel)
    self.catalogue = grpc.CatalogueServiceStub(self.channel)

  def close(self):
    self.channel.close()

  def get(self, name: str, user_id: int = 123):
    response = self.count.Get(
      model.CountFilter(
        user_id=user_id,
        name=name
      )
    )
    if response.status == model.Status.SUCCESS:
      logger.debug(f"Текущее значение: {response.value}")
      return True, response.value
    else:
      logger.error(f"Ошибка получения: {response.error.message}")
      return False, -1

  def increment(self, name: str, user_id: int = 123, delta: int = 1):
    increment_response = self.count.Increment(
      model.CountIncrementRequest(
        user_id=user_id,
        name=name,
        delta=delta
      )
    )

    if increment_response.status == model.Status.SUCCESS:
      logger.debug(f"Новое значение: {increment_response.value}")
      return True, increment_response.value
    else:
      logger.error(f"Ошибка инкремента: {increment_response.error.message}")
      return False, -1

  def create_count(self, name: str, description: str = "тестовый"):
    response = self.catalogue.Put(
      model.CataloguePutRequest(
        name=name,
        description=description
      )
    )

    if response.status == model.Status.SUCCESS:
      counter_id = response.value.id
      logger.debug(f"Создан счетчик ID: {counter_id}")
      return True, counter_id
    else:
      logger.error(f"Ошибка создания: {response.error.message}")
      return False, -1

  def update_count(self, id: int, name: str):
    response = self.catalogue.Update(
      model.CatalogueUpdateRequest(
        id=id,
        name=name
      )
    )

    if response.status == model.Status.SUCCESS:
      counter_id = response.value.id
      logger.debug(f"Обновлён счетчик ID: {counter_id}")
      return True, counter_id
    else:
      logger.error(f"Ошибка обновления: {response.error.message}")
      return False, -1
