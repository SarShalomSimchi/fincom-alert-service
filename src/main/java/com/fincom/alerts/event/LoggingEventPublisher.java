package com.fincom.alerts.event;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);
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
