package routenav.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader and writer built only on the Java standard library.
 *
 * Parsing turns text into plain Java values:
 * object -> LinkedHashMap, array -> ArrayList, string -> String,
 * number -> Double, true/false -> Boolean, null -> null.
 *
 * Writing accepts those same values plus any Number, Iterable or Enum.
 */
public final class Json {

    private Json() {
    }

    /**
     * Reads one JSON value from the text. Throws JsonException when the text
     * is missing, incomplete or not valid JSON.
     */
    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("No JSON text to read");
        }
        return new Parser(text).parseDocument();
    }

    /**
     * Turns a value into JSON text. Objects keep the order their keys were added.
     */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    /**
     * Creates an empty JSON object that keeps its key order.
     */
    public static Map<String, Object> newObject() {
        return new LinkedHashMap<>();
    }

    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            writeString((String) value, out);
        } else if (value instanceof Boolean) {
            out.append(value.toString());
        } else if (value instanceof Number) {
            writeNumber((Number) value, out);
        } else if (value instanceof Map) {
            writeObject((Map<?, ?>) value, out);
        } else if (value instanceof Iterable) {
            writeArray((Iterable<?>) value, out);
        } else if (value instanceof Enum) {
            writeString(((Enum<?>) value).name(), out);
        } else {
            throw new IllegalArgumentException("Cannot write this value as JSON: " + value.getClass());
        }
    }

    private static void writeObject(Map<?, ?> object, StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : object.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalArgumentException("JSON object keys must be text");
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString((String) entry.getKey(), out);
            out.append(':');
            writeValue(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeArray(Iterable<?> values, StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeValue(value, out);
        }
        out.append(']');
    }

    private static void writeNumber(Number number, StringBuilder out) {
        if (number instanceof Double || number instanceof Float) {
            double value = number.doubleValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("JSON cannot hold NaN or infinity");
            }
            // Double.toString keeps a decimal point, so 3.0 stays 3.0 like the
            // page expects, and 0.9 stays 0.9.
            out.append(Double.toString(value));
        } else {
            out.append(number.toString());
        }
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    /**
     * One pass over the text. It reads exactly one value and then complains if
     * anything except whitespace is left over.
     */
    private static final class Parser {
        private final String text;
        private int at;

        Parser(String text) {
            this.text = text;
        }

        Object parseDocument() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (at != text.length()) {
                throw error("Extra text after the JSON value");
            }
            return value;
        }

        private Object parseValue() {
            if (at >= text.length()) {
                throw error("The JSON ends too early");
            }
            char c = text.charAt(at);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                    return parseLiteral("true", Boolean.TRUE);
                case 'f':
                    return parseLiteral("false", Boolean.FALSE);
                case 'n':
                    return parseLiteral("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw error("Unexpected character '" + c + "'");
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            at++; // {
            skipWhitespace();
            if (take('}')) {
                return object;
            }
            while (true) {
                skipWhitespace();
                if (at >= text.length() || text.charAt(at) != '"') {
                    throw error("Expected a key in quotes");
                }
                String key = parseString();
                skipWhitespace();
                if (!take(':')) {
                    throw error("Expected ':' after the key");
                }
                skipWhitespace();
                object.put(key, parseValue());
                skipWhitespace();
                if (take('}')) {
                    return object;
                }
                if (!take(',')) {
                    throw error("Expected ',' or '}' in the object");
                }
            }
        }

        private List<Object> parseArray() {
            List<Object> values = new ArrayList<>();
            at++; // [
            skipWhitespace();
            if (take(']')) {
                return values;
            }
            while (true) {
                skipWhitespace();
                values.add(parseValue());
                skipWhitespace();
                if (take(']')) {
                    return values;
                }
                if (!take(',')) {
                    throw error("Expected ',' or ']' in the array");
                }
            }
        }

        private String parseString() {
            at++; // opening quote
            StringBuilder out = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') {
                    return out.toString();
                }
                if (c < 0x20) {
                    throw error("A control character has to be escaped");
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (at >= text.length()) {
                    throw error("The string ends after a backslash");
                }
                char escaped = text.charAt(at++);
                switch (escaped) {
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '/':
                        out.append('/');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'u':
                        out.append(parseUnicode());
                        break;
                    default:
                        throw error("Unknown escape '\\" + escaped + "'");
                }
            }
            throw error("The string has no closing quote");
        }

        private char parseUnicode() {
            if (at + 4 > text.length()) {
                throw error("A \\u escape needs four hex digits");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(at + i), 16);
                if (digit < 0) {
                    throw error("A \\u escape needs four hex digits");
                }
                value = value * 16 + digit;
            }
            at += 4;
            return (char) value;
        }

        private Double parseNumber() {
            int start = at;
            take('-');
            if (at >= text.length()) {
                throw error("A number has no digits");
            }
            if (take('0')) {
                if (at < text.length() && isDigit(text.charAt(at))) {
                    throw error("A number cannot start with a leading zero");
                }
            } else {
                readDigits();
            }
            if (take('.')) {
                readDigits();
            }
            if (at < text.length() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
                at++;
                if (!take('+')) {
                    take('-');
                }
                readDigits();
            }
            String number = text.substring(start, at);
            try {
                return Double.valueOf(number);
            } catch (NumberFormatException e) {
                throw error("The number " + number + " is too large");
            }
        }

        private void readDigits() {
            int start = at;
            while (at < text.length() && isDigit(text.charAt(at))) {
                at++;
            }
            if (at == start) {
                throw error("A number is missing its digits");
            }
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private Object parseLiteral(String word, Object value) {
            if (!text.startsWith(word, at)) {
                throw error("Expected " + word);
            }
            at += word.length();
            return value;
        }

        private boolean take(char expected) {
            if (at < text.length() && text.charAt(at) == expected) {
                at++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (at < text.length()) {
                char c = text.charAt(at);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    at++;
                } else {
                    return;
                }
            }
        }

        private JsonException error(String message) {
            return new JsonException(message + " (at position " + at + ")");
        }
    }
}
