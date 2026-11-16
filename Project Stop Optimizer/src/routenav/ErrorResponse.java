package routenav;

/**
 * The answer of POST /api/route when the trip cannot be planned.
 */
public class ErrorResponse {
    private final boolean success = false;
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
