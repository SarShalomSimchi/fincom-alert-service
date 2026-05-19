package com.fincom.alerts.event;


import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingEventPublisher implements EventPublisher {

    private final ObjectMapper objectMapper;

    @Override
    public void publish(AlertEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info(json);
        } catch (Exception ex) {
            log.error("Failed to serialize AlertEvent: {}", ex.getMessage(), ex);
        }
    }
}
