package com.lms.ledger.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.lms.ledger.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class LoanMgmtClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String baseUrl;

    public LoanMgmtClient(WebClient.Builder builder, @Value("${loan-mgmt.base-url:}") String baseUrl) {
        this.webClient = builder.build();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }

    public boolean configured() {
        return !baseUrl.isBlank();
    }

    public LoanContext context(String subscriberRef) {
        requireConfigured();
        return webClient.get()
                .uri(baseUrl + "/api/v1/internal/subscribers/{subscriberRef}/context", subscriberRef)
                .retrieve()
                .bodyToMono(LoanContext.class)
                .block(TIMEOUT);
    }

    public List<OutboxEvent> unpublishedAccounting() {
        requireConfigured();
        List<OutboxEvent> events = webClient.get()
                .uri(baseUrl + "/api/v1/internal/outbox?topic=accounting.events")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<OutboxEvent>>() {
                })
                .block(TIMEOUT);
        return events == null ? List.of() : events;
    }

    public void markPublished(long outboxId) {
        requireConfigured();
        webClient.post()
                .uri(baseUrl + "/api/v1/internal/outbox/{outboxId}/published", outboxId)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    private void requireConfigured() {
        if (!configured()) {
            throw ApiException.unavailable("LOAN_MGMT_URL_UNSET", "Set loan-mgmt.base-url before calling loan_mgmt");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanContext(String subscriberRef, List<LoanAdvance> advances) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanAdvance(
            UUID advanceId,
            String status,
            String marketCode,
            String currency,
            LocalDate dueDate,
            LocalDateTime drawnAt,
            BigDecimal principalAmount,
            BigDecimal feeAmount,
            BigDecimal principalResidual,
            BigDecimal feeResidual,
            List<LoanAllocation> allocations
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanAllocation(
            UUID allocationId,
            UUID postingId,
            UUID advanceId,
            String component,
            BigDecimal amount,
            Integer seq,
            LocalDateTime createdAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutboxEvent(
            Long outboxId,
            String topic,
            String messageKey,
            String eventType,
            JsonNode payload,
            LocalDateTime createdAt
    ) {
    }
}
