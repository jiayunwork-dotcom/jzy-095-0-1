package com.example.notchfatigue.validation;

import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchRequest;
import com.example.notchfatigue.model.SequenceRequest;
import com.example.notchfatigue.model.UnloadRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 输入校验测试：每条拒绝路径都要带原因。
 */
class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    private static NotchRequest request(Double e, Double k, Double n, Double kt, Double sn) {
        return new NotchRequest(new MaterialProperties(e, k, n), kt, sn);
    }

    @Test
    void acceptsValidRequest() {
        assertDoesNotThrow(() -> validator.validate(
                request(206000.0, 1200.0, 0.2, 3.0, 100.0)));
    }

    @Test
    void acceptsKtExactlyOne() {
        assertDoesNotThrow(() -> validator.validate(
                request(206000.0, 1200.0, 0.2, 1.0, 100.0)));
    }

    @Test
    void rejectsNonPositiveE() {
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(0.0, 1200.0, 0.2, 3.0, 100.0)));
        assertTrue(a.getMessage().contains("弹性模量"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(-1.0, 1200.0, 0.2, 3.0, 100.0)));
    }

    @Test
    void rejectsNonPositiveK() {
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 0.0, 0.2, 3.0, 100.0)));
        assertTrue(a.getMessage().contains("强度系数"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, -5.0, 0.2, 3.0, 100.0)));
    }

    @Test
    void rejectsNonPositiveN() {
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, 0.0, 3.0, 100.0)));
        assertTrue(a.getMessage().contains("硬化指数"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, -0.2, 3.0, 100.0)));
    }

    @Test
    void rejectsKtBelowOne() {
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, 0.2, 0.99, 100.0)));
        assertTrue(a.getMessage().contains("Kt"));
    }

    @Test
    void rejectsNegativeNominalStress() {
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, 0.2, 3.0, -1.0)));
        assertTrue(a.getMessage().contains("名义应力"));
    }

    @Test
    void rejectsNaNAndInfinity() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(Double.NaN, 1200.0, 0.2, 3.0, 100.0)));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, 0.2, Double.POSITIVE_INFINITY, 100.0)));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, 0.2, 3.0, Double.NaN)));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(request(206000.0, 1200.0, Double.NaN, 3.0, 100.0)));
    }

    @Test
    void rejectsMissingBodyAndMaterial() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate((NotchRequest) null));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new NotchRequest(null, 3.0, 100.0)));
    }

    @Test
    void validatesSequence() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(new SequenceRequest(
                        new MaterialProperties(206000.0, 1200.0, 0.2), 3.0, List.of())));
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(new SequenceRequest(
                        new MaterialProperties(206000.0, 1200.0, 0.2), 3.0, null)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(new SequenceRequest(
                        new MaterialProperties(206000.0, 1200.0, 0.2), 3.0,
                        java.util.Arrays.asList(10.0, null, 30.0))));
        assertTrue(ex.getMessage().contains("第 2"));
        assertDoesNotThrow(() -> validator.validate(new SequenceRequest(
                new MaterialProperties(206000.0, 1200.0, 0.2), 3.0,
                List.of(0.0, 50.0, 100.0))));
    }

    @Test
    void validatesUnload() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(new UnloadRequest(
                        new MaterialProperties(206000.0, 1200.0, 0.2), 3.0, 100.0, 200.0)));
        assertDoesNotThrow(() -> validator.validate(new UnloadRequest(
                new MaterialProperties(206000.0, 1200.0, 0.2), 3.0, 200.0, 100.0)));
    }
}
