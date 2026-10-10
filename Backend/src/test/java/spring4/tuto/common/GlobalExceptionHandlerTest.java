package spring4.tuto.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import spring4.tuto.common.exception.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void genericException_doesNotExposeInternalMessage() {
        var handler = new GlobalExceptionHandler();

        var response = handler.handleGenericException(new IllegalStateException("database password leaked"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void validationErrors_returnFailureResponse() {
        var handler = new GlobalExceptionHandler();
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "password", "Password is invalid"));

        var exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Password is invalid", response.getBody().getData().get("password"));
    }
}
