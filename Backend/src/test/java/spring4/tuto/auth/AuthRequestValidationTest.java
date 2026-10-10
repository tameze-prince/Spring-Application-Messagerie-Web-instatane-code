package spring4.tuto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import spring4.tuto.auth.dto.LoginRequest;
import spring4.tuto.auth.dto.RegisterRequest;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthRequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void validRegistrationMatchesTheFrontendContract() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice_01");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setFirstName("Alice");
        request.setLastName("Morgan");

        assertTrue(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void registrationRejectsInvalidValuesForEveryRequiredFieldAndNames() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setEmail("not-an-email");
        request.setPassword("short");
        request.setFirstName("a".repeat(81));
        request.setLastName("bad\nname");

        Set<String> invalidFields = VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertTrue(invalidFields.contains("username"));
        assertTrue(invalidFields.contains("email"));
        assertTrue(invalidFields.contains("password"));
        assertTrue(invalidFields.contains("firstName"));
        assertTrue(invalidFields.contains("lastName"));
    }

    @Test
    void loginUsesTheSamePasswordLengthPolicyAndBoundsLoginLength() {
        LoginRequest request = new LoginRequest();
        request.setLogin("alice");
        request.setPassword("short");

        Set<String> invalidFields = VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertTrue(invalidFields.contains("password"));

        request.setPassword("password123");
        request.setLogin("a".repeat(256));
        assertFalse(VALIDATOR.validate(request).isEmpty());
    }
}
