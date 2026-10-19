package com.lms.loan.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class LoanDtos {

    private LoanDtos() {
    }

    public record CreateDisclosureRequest(
            @NotBlank @Size(max = 64) String subscriberRef,
            @NotNull Integer pidVersion,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
            @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal fee,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal totalRepayable,
            @NotBlank @Size(max = 182) String renderedText,
            @NotBlank @Size(max = 128) String ackToken,
            Boolean truncated
    ) {
    }

    public record AcknowledgeRequest(@NotBlank @Size(max = 128) String ackToken) {
    }

    public record CreateMandateRequest(
            @NotBlank @Size(max = 64) String subscriberRef,
            @NotNull UUID disclosureId,
            @NotNull @DecimalMin(value = "0.000000", inclusive = false) @DecimalMax("1.000000")
            @Digits(integer = 1, fraction = 6) BigDecimal mandateShare
    ) {
    }

    public record CreateAdvanceRequest(
            @NotNull UUID disclosureId,
            UUID mandateId,
            @NotBlank @Size(max = 8) String marketCode,
            @NotBlank @Size(max = 64) String subscriberRef,
            @NotBlank String msisdnHashHex,
            @NotNull LocalDateTime msisdnValidFrom,
            @NotBlank @Size(max = 128) String originationKey,
            @NotBlank String productType,
            @NotBlank @Size(max = 64) String skuId,
            @NotBlank @Size(max = 64) String pidId,
            @NotNull Integer pidVersion,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotBlank @Size(max = 64) String lenderOfRecord,
            @NotNull Boolean regulatedFlag,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal principalAmount,
            @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal feeAmount,
            @NotBlank String feeMethod,
            @NotNull LocalDate dueDate,
            @NotNull @Min(0) Integer graceDays,
            LocalDateTime drawnAt
    ) {
    }

    public record CreditResultRequest(@NotBlank String outcome) {
    }

    public record CancelRequest(@NotNull @Positive Integer coolingOffHours) {
    }

    public record RaiseDisputeRequest(@NotBlank String disputeType) {
    }

    public record TransitionDisputeRequest(@NotBlank String status) {
    }

    public record QuoteRequest(
            @NotBlank @Size(max = 128) String signalId,
            @NotBlank @Size(max = 64) String subscriberRef,
            @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal grossRecharge,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotNull Boolean sweepable,
            @NotNull @DecimalMin(value = "0.000000", inclusive = false) @DecimalMax("1.000000")
            @Digits(integer = 1, fraction = 6) BigDecimal maxShare,
            @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal crossProductHeadroom,
            @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal neverStrandFloor
    ) {
    }

    public record SettleRequest(
            @NotBlank @Size(max = 128) String signalId,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal settledAmount,
            @NotNull Boolean authorised,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @Size(max = 128) String idempotencyKey,
            LocalDateTime settledAt
    ) {
    }

    public record ReleaseRequest(
            @NotBlank @Size(max = 128) String signalId,
            @NotBlank @Size(max = 32) String refusalCode
    ) {
    }

    public record VoluntaryPaymentRequest(
            @NotBlank @Size(max = 64) String subscriberRef,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @Size(max = 128) String idempotencyKey
    ) {
    }

    public record DunningTickRequest(
            LocalDateTime asOf,
            @NotNull @Positive Integer writeOffDays
    ) {
    }

    public record DisclosureView(
            UUID disclosureId,
            String subscriberRef,
            UUID advanceId,
            Integer pidVersion,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalRepayable,
            String renderedText,
            String ackToken,
            LocalDateTime acknowledgedAt
    ) {
    }

    public record MandateView(
            UUID mandateId,
            String subscriberRef,
            UUID disclosureId,
            BigDecimal mandateShare,
            String status,
            LocalDateTime armedAt,
            LocalDateTime revokedAt
    ) {
    }

    public record DunningView(int daysPastDue, int agingBucket, int ladderStep, LocalDateTime updatedAt) {
    }

    public record AllocationView(
            UUID allocationId,
            UUID postingId,
            UUID advanceId,
            String component,
            BigDecimal amount,
            int seq,
            LocalDateTime createdAt
    ) {
    }

    public record AdvanceView(
            UUID advanceId,
            String marketCode,
            String subscriberRef,
            String msisdnHashHex,
            LocalDateTime msisdnValidFrom,
            String originationKey,
            String productType,
            String skuId,
            String pidId,
            Integer pidVersion,
            String currency,
            String lenderOfRecord,
            Boolean regulatedFlag,
            BigDecimal principalAmount,
            BigDecimal feeAmount,
            String feeMethod,
            BigDecimal principalResidual,
            BigDecimal feeResidual,
            String status,
            LocalDateTime drawnAt,
            LocalDate dueDate,
            LocalDateTime graceDatetime,
            LocalDateTime writtenOffAt,
            Long version,
            DunningView dunning,
            List<AllocationView> allocations
    ) {
    }

    public record ScheduleView(UUID eventId, UUID advanceId, String eventType, String payload, LocalDateTime occurredAt) {
    }

    public record DisputeView(
            UUID disputeId,
            UUID advanceId,
            String disputeType,
            String status,
            boolean holdActive,
            LocalDateTime raisedAt,
            LocalDateTime resolvedAt
    ) {
    }

    public record ObligationView(
            UUID advanceId,
            LocalDate dueDate,
            BigDecimal feeResidual,
            BigDecimal principalResidual,
            String status
    ) {
    }

    public record QuoteView(
            UUID intentId,
            String signalId,
            String subscriberRef,
            String status,
            BigDecimal grossRecharge,
            BigDecimal collectable,
            BigDecimal quotedAmount,
            String refusalCode,
            UUID reservationId,
            List<ObligationView> obligations
    ) {
    }

    public record SettlementView(
            UUID intentId,
            String signalId,
            String status,
            UUID postingId,
            LocalDateTime settledAt,
            BigDecimal settledAmount,
            BigDecimal allocatedAmount,
            BigDecimal unallocatedAmount,
            boolean authorised,
            List<AllocationView> allocations
    ) {
    }

    public record SignalView(
            QuoteView quote,
            List<ReservationView> reservations,
            List<AttemptView> attempts
    ) {
    }

    public record ReservationView(
            UUID reservationId,
            String signalId,
            String verb,
            BigDecimal amount,
            String currency,
            String refusalCode,
            LocalDateTime createdAt
    ) {
    }

    public record AttemptView(UUID attemptId, UUID intentId, String outcome, String conductRecord, LocalDateTime createdAt) {
    }

    public record DunningTickView(int examined, int graceEntered, int expired, int writtenOff, List<UUID> writtenOffAdvanceIds) {
    }

    public record OutboxView(
            Long outboxId,
            String topic,
            String messageKey,
            String eventType,
            com.fasterxml.jackson.databind.JsonNode payload,
            LocalDateTime createdAt
    ) {
    }

    public record SubscriberContext(
            String subscriberRef,
            List<AdvanceView> advances
    ) {
    }
}
