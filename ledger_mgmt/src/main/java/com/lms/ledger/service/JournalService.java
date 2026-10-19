package com.lms.ledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.ledger.api.ApiException;
import com.lms.ledger.client.LoanMgmtClient;
import com.lms.ledger.domain.Chart;
import com.lms.ledger.domain.JournalBalancer;
import com.lms.ledger.domain.Support;
import com.lms.ledger.domain.entity.Journal;
import com.lms.ledger.domain.entity.JournalLine;
import com.lms.ledger.repo.JournalLineRepository;
import com.lms.ledger.repo.JournalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalLineRepository lineRepository;

    public JournalService(JournalRepository journalRepository, JournalLineRepository lineRepository) {
        this.journalRepository = journalRepository;
        this.lineRepository = lineRepository;
    }

    @Transactional
    public JournalResult post(String sourceEventId, UUID advanceId, String journalType, String marketCode,
                              String currency, List<Chart.Line> lines, LocalDateTime postedAt) {
        Journal existing = journalRepository.findBySourceEventId(sourceEventId).orElse(null);
        if (existing != null) {
            return new JournalResult(toView(existing), false);
        }
        try {
            JournalBalancer.requireBalanced(lines);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("UNBALANCED_JOURNAL", exception.getMessage());
        }
        Journal journal = new Journal();
        journal.setJournalId(Support.uuidV7());
        journal.setSourceEventId(sourceEventId);
        journal.setAdvanceId(advanceId);
        journal.setJournalType(journalType);
        journal.setMarketCode(marketCode);
        journal.setCurrency(currency);
        journal.setPostedAt(postedAt == null ? Support.now() : postedAt);
        journalRepository.save(journal);
        for (Chart.Line line : lines) {
            JournalLine row = new JournalLine();
            row.setLineId(Support.uuidV7());
            row.setJournalId(journal.getJournalId());
            row.setAccountCode(line.accountCode());
            row.setSide(line.side());
            row.setAmount(Support.money(line.amount()));
            lineRepository.save(row);
        }
        return new JournalResult(toView(journal), true);
    }

    @Transactional
    public JournalResult bookAccountingEvent(LoanMgmtClient.OutboxEvent event) {
        JsonNode payload = event.payload();
        if (payload == null || payload.isNull()) {
            throw ApiException.badRequest("PAYLOAD_FAILED", "Accounting event " + event.outboxId() + " has no payload");
        }
        String journalType = mapType(text(payload, "journalType"), event.eventType());
        UUID advanceId = UUID.fromString(text(payload, "advanceId"));
        String marketCode = text(payload, "marketCode");
        String currency = text(payload, "currency");
        BigDecimal fee = money(payload, "feeAmount");
        BigDecimal principal = money(payload, "principalAmount");
        BigDecimal amount = money(payload, "amount");
        List<Chart.Line> lines;
        try {
            lines = Chart.lines(journalType, fee, principal, amount);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("A journal needs")) {
                return new JournalResult(null, false);
            }
            throw ApiException.badRequest("JOURNAL_TYPE_UNSUPPORTED", exception.getMessage());
        }
        return post("outbox:" + event.outboxId(), advanceId, journalType, marketCode, currency, lines, event.createdAt());
    }

    @Transactional(readOnly = true)
    public JournalView get(UUID journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> ApiException.notFound("JOURNAL_NOT_FOUND", "Journal not found"));
        return toView(journal);
    }

    @Transactional(readOnly = true)
    public List<JournalView> find(List<UUID> advanceIds, String marketCode) {
        List<Journal> journals;
        if (advanceIds != null) {
            if (advanceIds.isEmpty()) {
                return List.of();
            }
            journals = journalRepository.findByAdvanceIdInOrderByPostedAtAsc(advanceIds);
        } else if (marketCode != null && !marketCode.isBlank()) {
            journals = journalRepository.findByMarketCodeOrderByPostedAtAsc(marketCode.trim());
        } else {
            journals = journalRepository.findAllByOrderByPostedAtAsc();
        }
        return journals.stream().map(this::toView).toList();
    }

    private JournalView toView(Journal journal) {
        List<LineView> lines = lineRepository.findByJournalId(journal.getJournalId()).stream()
                .sorted(Comparator.comparingInt((JournalLine line) -> "DR".equals(line.getSide()) ? 0 : 1)
                        .thenComparing(JournalLine::getAccountCode))
                .map(line -> new LineView(line.getLineId(), line.getAccountCode(), line.getSide(), Support.money(line.getAmount()).toPlainString()))
                .toList();
        return new JournalView(journal.getJournalId(), journal.getSourceEventId(), journal.getAdvanceId(),
                journal.getJournalType(), journal.getMarketCode(), journal.getCurrency(),
                journal.getPostedAt().toString(), lines);
    }

    static String mapType(String journalType, String eventType) {
        if (journalType != null && !journalType.isBlank()) {
            return journalType.trim();
        }
        if ("FEE_BOOKED".equals(eventType)) {
            return "FEE_REVENUE";
        }
        if (eventType == null || eventType.isBlank()) {
            throw ApiException.badRequest("JOURNAL_TYPE_REQUIRED", "Accounting payload has no journalType");
        }
        return eventType.trim();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw ApiException.badRequest("PAYLOAD_FAILED", "Accounting payload is missing " + field);
        }
        return value.asText();
    }

    private static BigDecimal money(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return Support.zero();
        }
        return Support.money(new BigDecimal(value.asText()));
    }

    public record JournalResult(JournalView journal, boolean created) {
    }

    public record JournalView(
            UUID journalId,
            String sourceEventId,
            UUID advanceId,
            String journalType,
            String marketCode,
            String currency,
            String postedAt,
            List<LineView> lines
    ) {
    }

    public record LineView(UUID lineId, String accountCode, String side, String amount) {
    }

    public List<Chart.Line> manualLines(List<ManualLine> lines) {
        List<Chart.Line> mapped = new ArrayList<>();
        for (ManualLine line : lines) {
            String side = line.side().trim().toUpperCase();
            mapped.add(new Chart.Line(line.accountCode().trim(), side, Support.money(line.amount())));
        }
        return mapped;
    }

    public record ManualLine(String accountCode, String side, BigDecimal amount) {
    }
}
