package com.lms.loan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.loan.api.ApiException;
import com.lms.loan.domain.Utc;
import com.lms.loan.domain.entity.OutboxEvent;
import com.lms.loan.repo.OutboxRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OutboxWriter {

    public static final String LOAN_EVENTS = "loan.events";
    public static final String ACCOUNTING_EVENTS = "accounting.events";
    public static final String MANDATE_EVENTS = "mandate.events";
    public static final String DISPUTE_EVENTS = "dispute.events";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void append(String topic, String messageKey, String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setTopic(topic);
        event.setMessageKey(messageKey);
        event.setEventType(eventType);
        event.setPayload(json(payload));
        event.setCreatedAt(Utc.now());
        outboxRepository.save(event);
    }

    public void loan(UUID advanceId, String eventType, Object payload) {
        append(LOAN_EVENTS, advanceId.toString(), eventType, payload);
    }

    public void accounting(UUID advanceId, String eventType, Object payload) {
        append(ACCOUNTING_EVENTS, advanceId.toString(), eventType, payload);
    }

    private String json(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_FAILED", "Could not write outbox payload");
        }
    }
}
