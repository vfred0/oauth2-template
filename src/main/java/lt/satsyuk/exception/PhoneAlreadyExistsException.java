package lt.satsyuk.exception;

public class PhoneAlreadyExistsException extends RuntimeException {
    public PhoneAlreadyExistsException(String phone) {
        super("Client with phone=" + phone + " already exists");
    }
}