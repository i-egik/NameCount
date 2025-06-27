import os
import unittest
import uuid
from time import sleep

import named_count

NAME = 'TEST'
TIMEOUT_SEC = 1
TESTNAME = str(uuid.uuid4()).replace('-', '').upper()
TEST_DEFAULT_VALUE = str(uuid.uuid4()).replace('-', '').upper()

class Tests(unittest.TestCase):
  @classmethod
  def setUpClass(cls):
    cls.nc = named_count.NamedCount()
    cls.pg = named_count.PgNamedCount()
    cls.pg.connect()

  @classmethod
  def tearDownClass(cls):
    cls.pg.close()

  def test_create(self):
    (ok, count_id, default_value) = self.nc.create_count(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(count_id)
    (ok, pg_count_id) = self.pg.create_count(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(pg_count_id)
    self.assertEqual(count_id, pg_count_id)

  def test_create_default_value(self):
    (ok, count_id, default_value) = self.nc.create_count(
      TEST_DEFAULT_VALUE, "with_default_value", 100)
    self.assertEqual(True, ok)
    self.assertIsNotNone(count_id)
    self.assertIsNotNone(default_value)
    self.assertEqual(100, default_value)

  def test_catalogue_update(self):
    (ok, count_id, default_value) = self.nc.create_count(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(count_id)
    (ok, struct) = self.nc.update_count(count_id, TESTNAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(struct)
    self.assertEqual(TESTNAME, struct[1])

  def test_increment(self):
    (ok, increment) = self.nc.increment(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(increment)
    (ok, value) = self.nc.get(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(value)
    (ok, increment) = self.nc.increment(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(increment)
    self.assertEqual(value + 1, increment)
    sleep(TIMEOUT_SEC)
    (ok, value) = self.pg.get(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(value)
    self.assertEqual(increment, value)

  def test_reset(self):
    (ok, value) = self.nc.get(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(value)
    (ok, reset) = self.nc.reset(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(reset)
    self.assertEqual(0, reset)
    (ok, value) = self.nc.get(NAME)
    self.assertEqual(True, ok)
    self.assertIsNotNone(value)
    self.assertEqual(0, value)

os.chdir("/Users/pastor/github/named-count")

if __name__ == '__main__':
  unittest.main()
