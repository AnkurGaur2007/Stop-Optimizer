package routenav.json;

/**
 * Thrown when text cannot be read as JSON. The HTTP layer turns it into the
 * same "Could not read the request." answer the old framework produced.
 */
public class JsonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonException(String message) {
        super(message);
    }
}
