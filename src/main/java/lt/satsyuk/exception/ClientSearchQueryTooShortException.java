package lt.satsyuk.exception;

import lombok.Getter;

@Getter
public class ClientSearchQueryTooShortException extends RuntimeException {

    private final int minLength;

    public ClientSearchQueryTooShortException(int minLength) {
        super("Search query must contain at least " + minLength + " characters");
        this.minLength = minLength;
    }

    public String getMessageCode() {
        return "error.client.searchQueryTooShort";
    }
}

