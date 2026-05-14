package com.fincom.alerts.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fincom.alerts.exception.AlertAlreadyDecidedException;
import com.fincom.alerts.exception.InvalidTransitionException;

class AlertValidatorTest {

    private final AlertValidator validator = new AlertValidator();

    @Test
    void givenAlertWithStatusOpen_whenValidateEscalate_thenSucceeds() {
        Alert alert = Alert.builder().status(AlertStatus.OPEN).build();
        assertDoesNotThrow(() -> validator.validateEscalate(alert));
    }

    @Test
    void givenAlertWithStatusEscalated_whenValidateEscalate_thenThrowsInvalidTransition() {
        Alert alert = Alert.builder().status(AlertStatus.ESCALATED).build();
        assertThrows(InvalidTransitionException.class, () -> validator.validateEscalate(alert));
    }

    @Test
    void givenAlertWithStatusCleared_whenValidateEscalate_thenThrowsInvalidTransition() {
        Alert alert = Alert.builder().status(AlertStatus.CLEARED).build();
        assertThrows(InvalidTransitionException.class, () -> validator.validateEscalate(alert));
    }

    @Test
    void givenAlertWithStatusConfirmedHit_whenValidateEscalate_thenThrowsInvalidTransition() {
        Alert alert = Alert.builder().status(AlertStatus.CONFIRMED_HIT).build();
        assertThrows(InvalidTransitionException.class, () -> validator.validateEscalate(alert));
    }

    @Test
    void givenAlertWithStatusOpen_whenValidateDecide_thenSucceeds() {
        Alert alert = Alert.builder().status(AlertStatus.OPEN).build();
        assertDoesNotThrow(() -> validator.validateDecide(alert));
    }

    @Test
    void givenAlertWithStatusEscalated_whenValidateDecide_thenSucceeds() {
        Alert alert = Alert.builder().status(AlertStatus.ESCALATED).build();
        assertDoesNotThrow(() -> validator.validateDecide(alert));
    }

    @Test
    void givenAlertWithStatusCleared_whenValidateDecide_thenThrowsAlreadyDecided() {
        Alert alert = Alert.builder().status(AlertStatus.CLEARED).build();
        assertThrows(AlertAlreadyDecidedException.class, () -> validator.validateDecide(alert));
    }

    @Test
    void givenAlertWithStatusConfirmedHit_whenValidateDecide_thenThrowsAlreadyDecided() {
        Alert alert = Alert.builder().status(AlertStatus.CONFIRMED_HIT).build();
        assertThrows(AlertAlreadyDecidedException.class, () -> validator.validateDecide(alert));
    }
}