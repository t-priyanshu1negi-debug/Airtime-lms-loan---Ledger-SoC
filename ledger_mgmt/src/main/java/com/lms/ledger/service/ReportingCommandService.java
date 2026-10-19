package com.lms.ledger.service;

import com.lms.ledger.api.ApiException;
import com.lms.ledger.client.LoanMgmtClient;
import com.lms.ledger.domain.Support;
import com.lms.ledger.domain.entity.AuditChain;
import com.lms.ledger.domain.entity.BilledKey;
import com.lms.ledger.domain.entity.BilledVsCollected;
import com.lms.ledger.domain.entity.ProjectionWatermark;
import com.lms.ledger.domain.entity.ReconWindow;
import com.lms.ledger.domain.entity.WatermarkKey;
import com.lms.ledger.repo.AuditChainRepository;
import com.lms.ledger.repo.BilledVsCollectedRepository;
import com.lms.ledger.repo.ReconWindowRepository;
import com.lms.ledger.repo.WatermarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class ReportingCommandService {

    private final ReconWindowRepository reconWindowRepository;
    private final BilledVsCollectedRepository billedRepository;
    private final AuditChainRepository auditChainRepository;
    private final WatermarkRepository watermarkRepository;
    private final Object chainMonitor = new Object();

    public ReportingCommandService(ReconWindowRepository reconWindowRepository,
                                   BilledVsCollectedRepository billedRepository,
                                   AuditChainRepository auditChainRepository,
                                   WatermarkRepository watermarkRepository) {
        this.reconWindowRepository = reconWindowRepository;
        this.billedRepository = billedRepository;
        this.auditChainRepository = auditChainRepository;
        this.watermarkRepository = watermarkRepository;
    }

    @Transactional
    public ReconWindow openWindow(LocalDateTime start, LocalDateTime end, BigDecimal quoted, BigDecimal ingested,
                                  BigDecimal posted, long filteredCount, Integer breakCount, String exceptionOwner) {
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("WINDOW_INVALID", "windowEnd must be after windowStart");
        }
        ReconWindow row = new ReconWindow();
        row.setWindowId(Support.uuidV7());
        row.setWindowStart(start);
        row.setWindowEnd(end);
        row.setQuotedTotal(Support.money(quoted));
        row.setIngestedTotal(Support.money(ingested));
        row.setPostedTotal(Support.money(posted));
        row.setFilteredCount(filteredCount);
        int breaks = breakCount == null
                ? (quoted.compareTo(ingested) == 0 && ingested.compareTo(posted) == 0 ? 0 : 1)
                : breakCount;
        row.setBreakCount(breaks);
        row.setExceptionOwner(exceptionOwner);
        return reconWindowRepository.save(row);
    }

    @Transactional
    public BilledVsCollected upsertBilled(String rail, LocalDate businessDate, BigDecimal billed, BigDecimal collected) {
        BilledKey key = new BilledKey(rail.trim(), businessDate);
        BilledVsCollected row = billedRepository.findById(key).orElseGet(BilledVsCollected::new);
        row.setId(key);
        row.setBilled(Support.money(billed));
        row.setCollected(Support.money(collected));
        return billedRepository.save(row);
    }

    @Transactional
    public AuditChain appendAudit(String entityType, String entityId, String action, String payload) {
        synchronized (chainMonitor) {
            AuditChain head = auditChainRepository.findFirstByOrderBySeqDesc();
            byte[] previous = head == null ? new byte[32] : Arrays.copyOf(head.getChainHash(), head.getChainHash().length);
            byte[] payloadHash = sha256(payload.getBytes(StandardCharsets.UTF_8));
            byte[] chain = sha256(concat(previous, payloadHash));
            AuditChain row = new AuditChain();
            row.setEntityType(entityType.trim());
            row.setEntityId(entityId.trim());
            row.setAction(action.trim());
            row.setPayloadHash(payloadHash);
            row.setPrevHash(previous);
            row.setChainHash(chain);
            row.setCreatedAt(Support.now());
            return auditChainRepository.saveAndFlush(row);
        }
    }

    @Transactional
    public ProjectionWatermark upsertWatermark(String projectionName, String topic, int partitionNo, long lastOffset) {
        WatermarkKey key = new WatermarkKey(projectionName.trim(), topic.trim(), partitionNo);
        ProjectionWatermark row = watermarkRepository.findById(key).orElseGet(ProjectionWatermark::new);
        row.setId(key);
        row.setLastOffset(lastOffset);
        row.setUpdatedAt(Support.now());
        return watermarkRepository.save(row);
    }

    public void noteIngest(LoanMgmtClient.OutboxEvent event) {
        long offset = event.outboxId() == null ? 0L : event.outboxId();
        upsertWatermark("account_ledger", "accounting.events", 0, offset);
    }

    public static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static byte[] concat(byte[] left, byte[] right) {
        byte[] combined = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, combined, left.length, right.length);
        return combined;
    }
}
