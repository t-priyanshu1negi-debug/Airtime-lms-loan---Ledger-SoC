package com.lms.ledger.api;

import com.lms.ledger.domain.Chart;
import com.lms.ledger.domain.Support;
import com.lms.ledger.domain.entity.AuditChain;
import com.lms.ledger.domain.entity.BilledVsCollected;
import com.lms.ledger.domain.entity.ProjectionWatermark;
import com.lms.ledger.domain.entity.ReconWindow;
import com.lms.ledger.service.AccountingIngestService;
import com.lms.ledger.service.JournalService;
import com.lms.ledger.service.ReportingCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LedgerController {

    private final JournalService journalService;
    private final ReportingCommandService reportingCommandService;
    private final AccountingIngestService accountingIngestService;

    public LedgerController(JournalService journalService,
                            ReportingCommandService reportingCommandService,
                            AccountingIngestService accountingIngestService) {
        this.journalService = journalService;
        this.reportingCommandService = reportingCommandService;
        this.accountingIngestService = accountingIngestService;
    }

    @PostMapping("/journals")
    public ResponseEntity<JournalService.JournalView> postJournal(@Valid @RequestBody PostJournalRequest request) {
        List<Chart.Line> lines = request.lines().stream()
                .map(line -> new Chart.Line(line.accountCode().trim(), line.side().trim().toUpperCase(), Support.money(line.amount())))
                .toList();
        JournalService.JournalResult result = journalService.post(
                request.sourceEventId().trim(), request.advanceId(), request.journalType().trim(),
                request.marketCode().trim(), request.currency().trim().toUpperCase(), lines, request.postedAt());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.journal());
    }

    @GetMapping("/journals/{journalId}")
    public JournalService.JournalView journal(@PathVariable UUID journalId) {
        return journalService.get(journalId);
    }

    @PostMapping("/recon-windows")
    public ResponseEntity<ReconView> recon(@Valid @RequestBody ReconRequest request) {
        ReconWindow row = reportingCommandService.openWindow(request.windowStart(), request.windowEnd(),
                request.quotedTotal(), request.ingestedTotal(), request.postedTotal(), request.filteredCount(),
                request.breakCount(), request.exceptionOwner());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReconView.from(row));
    }

    @PostMapping("/billed-vs-collected")
    public BilledView billed(@Valid @RequestBody BilledRequest request) {
        BilledVsCollected row = reportingCommandService.upsertBilled(request.rail(), request.businessDate(),
                request.billed(), request.collected());
        return BilledView.from(row);
    }

    @PostMapping("/audit-chain")
    public ResponseEntity<AuditView> audit(@Valid @RequestBody AuditRequest request) {
        AuditChain row = reportingCommandService.appendAudit(request.entityType(), request.entityId(),
                request.action(), request.payload());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuditView.from(row));
    }

    @PutMapping("/watermarks")
    public WatermarkView watermark(@Valid @RequestBody WatermarkRequest request) {
        ProjectionWatermark row = reportingCommandService.upsertWatermark(request.projectionName(), request.topic(),
                request.partitionNo(), request.lastOffset());
        return WatermarkView.from(row);
    }

    @PostMapping("/accounting/ingest")
    public AccountingIngestService.IngestResult ingest() {
        return accountingIngestService.ingest();
    }

    public record PostJournalRequest(
            @NotBlank @Size(max = 128) String sourceEventId,
            @NotNull UUID advanceId,
            @NotBlank @Size(max = 32) String journalType,
            @NotBlank @Size(max = 8) String marketCode,
            @NotBlank @Size(min = 3, max = 3) String currency,
            LocalDateTime postedAt,
            @NotEmpty List<LineRequest> lines
    ) {
    }

    public record LineRequest(
            @NotBlank @Size(max = 32) String accountCode,
            @NotBlank @Size(min = 2, max = 2) String side,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount
    ) {
    }

    public record ReconRequest(
            @NotNull LocalDateTime windowStart,
            @NotNull LocalDateTime windowEnd,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal quotedTotal,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal ingestedTotal,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal postedTotal,
            @NotNull Long filteredCount,
            Integer breakCount,
            @Size(max = 64) String exceptionOwner
    ) {
    }

    public record BilledRequest(
            @NotBlank @Size(max = 32) String rail,
            @NotNull LocalDate businessDate,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal billed,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal collected
    ) {
    }

    public record AuditRequest(
            @NotBlank @Size(max = 32) String entityType,
            @NotBlank @Size(max = 64) String entityId,
            @NotBlank @Size(max = 32) String action,
            @NotBlank String payload
    ) {
    }

    public record WatermarkRequest(
            @NotBlank @Size(max = 64) String projectionName,
            @NotBlank @Size(max = 64) String topic,
            @NotNull Integer partitionNo,
            @NotNull Long lastOffset
    ) {
    }

    public record ReconView(UUID windowId, LocalDateTime windowStart, LocalDateTime windowEnd,
                            BigDecimal quotedTotal, BigDecimal ingestedTotal, BigDecimal postedTotal,
                            long filteredCount, int breakCount, String exceptionOwner) {
        static ReconView from(ReconWindow row) {
            return new ReconView(row.getWindowId(), row.getWindowStart(), row.getWindowEnd(),
                    row.getQuotedTotal(), row.getIngestedTotal(), row.getPostedTotal(),
                    row.getFilteredCount(), row.getBreakCount(), row.getExceptionOwner());
        }
    }

    public record BilledView(String rail, LocalDate businessDate, BigDecimal billed, BigDecimal collected) {
        static BilledView from(BilledVsCollected row) {
            return new BilledView(row.getId().getRail(), row.getId().getBusinessDate(), row.getBilled(), row.getCollected());
        }
    }

    public record AuditView(long seq, String entityType, String entityId, String action,
                            String payloadHash, String prevHash, String chainHash) {
        static AuditView from(AuditChain row) {
            HexFormat hex = HexFormat.of();
            return new AuditView(row.getSeq(), row.getEntityType(), row.getEntityId(), row.getAction(),
                    hex.formatHex(row.getPayloadHash()), hex.formatHex(row.getPrevHash()), hex.formatHex(row.getChainHash()));
        }
    }

    public record WatermarkView(String projectionName, String topic, int partitionNo, long lastOffset) {
        static WatermarkView from(ProjectionWatermark row) {
            return new WatermarkView(row.getId().getProjectionName(), row.getId().getTopic(),
                    row.getId().getPartitionNo(), row.getLastOffset());
        }
    }
}
