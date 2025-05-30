package ru.pastor.templates.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Property")
class PropertyTest {

    private enum Maybe {
        STRING, BOOLEAN, INTEGER, LONG, DOUBLE, OBJECT
    }

    private record Element(Maybe maybe, Object expected, String value) {

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ElementObject(@JsonProperty("text") String _string, @JsonProperty("number") Long _long) {

    }

    private static Stream<Arguments> successfulWithoutDefault() {
        return Stream.of(
                Arguments.of(Maybe.STRING, "text", "text"),
                Arguments.of(Maybe.BOOLEAN, true, "true"),
                Arguments.of(Maybe.BOOLEAN, false, "false"),
                Arguments.of(Maybe.BOOLEAN, false, "unknown"),
                Arguments.of(Maybe.INTEGER, 0, "0"),
                Arguments.of(Maybe.LONG, 0L, "0"),
                Arguments.of(Maybe.DOUBLE, 0., "0."),
                Arguments.of(Maybe.OBJECT, new ElementObject("text", 0L), "{\"text\":\"text\",\"number\":0}")
        );
    }

    private static Stream<Arguments> successfulAttributes() {
        LocalDateTime now = LocalDateTime.now();
        return Stream.of(
                Arguments.of(Attribute.Default.EXPIRES, now, Map.<String, Object>of("expires", now)),
                Arguments.of(Attribute.Default.REFRESH, 4500, Map.<String, Object>of("refresh", 4500))
        );
    }

    @DisplayName("Атрибуты")
    @ParameterizedTest
    @MethodSource("successfulAttributes")
    void testSuccessAttributes(Attribute attribute, Object expected, Map<String, Object> attributes) {
        Property property = Property.of("key", "value", new ObjectMapper(), attributes);
        Optional<Object> object = property.attribute(attribute);
        assertEquals(expected, object.orElse(null));
    }

    @DisplayName("Без параметров по умолчанию")
    @ParameterizedTest
    @MethodSource("successfulWithoutDefault")
    void testSuccessfulWithoutDefault(Maybe maybe, Object expected, String value) {
        Property property = Property.of("_key", value, new ObjectMapper());
        switch (maybe) {
            case STRING -> {
                String text = property.asString();
                assertEquals(expected, text, String.format("'%s' not equal to '%s'", text, expected));
            }
            case BOOLEAN -> {
                boolean result = property.asBoolean();
                assertEquals(expected, result, String.format("'%s' not equal to '%s'", result, expected));
            }
            case INTEGER -> {
                int result = property.asInt(-1);
                assertEquals(expected, result, String.format("'%d' not equal to '%s'", result, expected));
            }
            case LONG -> {
                long result = property.asLong(-1);
                assertEquals(expected, result, String.format("'%d' not equal to '%s'", result, expected));
            }
            case DOUBLE -> {
                double result = property.asDouble(-1);
                assertEquals(expected, result, String.format("'%f' not equal to '%s'", result, expected));
            }
            case OBJECT -> {
                Optional<ElementObject> object = property.asObject(ElementObject.class);
                assertEquals(expected, object.orElse(null));
            }
        }
    }
}
