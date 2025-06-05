package ru.pastor.templates.named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Proxy")
public class ProxyTest {

  private UsageTesting usageTesting;

  @BeforeEach
  void setUp() {
    Testing testing = (Testing) Proxy.newProxyInstance(
      ProxyTest.class.getClassLoader(), new Class[]{Testing.class},
      (proxy, method, args) -> {
        return "test";
      });
    usageTesting = new UsageTesting(testing);
  }

  @Test
  void testSimple() {
    String resultString = usageTesting.test("One");
    assertEquals("Usagetesttest", resultString, "Incorrect result string");
  }

  private interface Testing {
    String test(String input);

    String test2(String input);
  }

  private record UsageTesting(Testing testing) {

    private String test(String input) {
      return "Usage" + testing.test(input) + testing.test2(input);
    }
  }
}
