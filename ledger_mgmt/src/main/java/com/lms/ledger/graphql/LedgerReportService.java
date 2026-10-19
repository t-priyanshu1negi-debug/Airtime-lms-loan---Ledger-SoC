package com.lms.ledger.graphql;

import com.lms.ledger.client.LoanMgmtClient;
import com.lms.ledger.domain.Support;
import com.lms.ledger.domain.entity.AuditChain;
import com.lms.ledger.domain.entity.BilledVsCollected;
import com.lms.ledger.domain.entity.ProjectionWatermark;
import com.lms.ledger.domain.entity.ReconWindow;
import com.lms.ledger.repo.AuditChainRepository;
import com.lms.ledger.repo.BilledVsCollectedRepository;
import com.lms.ledger.repo.ReconWindowRepository;
import com.lms.ledger.repo.WatermarkRepository;
import com.lms.ledger.service.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerReportService {

    private static final int CAP = 500;

    private final JournalService journalService;
    private final ReconWindowRepository reconWindowRepository;
    private final BilledVsCollectedRepository billedRepository;
    private final AuditChainRepository auditChainRepository;
    private final WatermarkRepository watermarkRepository;
    private final LoanMgmtClient loanMgmtClient;

    public LedgerReportService(JournalService journalService,
                               ReconWindowRepository reconWindowRepository,
                               BilledVsCollectedRepository billedRepository,
                               AuditChainRepository auditChainRepository,
                               WatermarkRepository watermarkRepository,
                               LoanMgmtClient loanMgmtClient) {
        this.journalService = journalService;
        this.reconWindowRepository = reconWindowRepository;
        this.billedRepository = billedRepository;
        this.auditChainRepository = auditChainRepository;
        this.watermarkRepository = watermarkRepository;
        this.loanMgmtClient = loanMgmtClient;
    }

    @Transactional(readOnly = true)
    public ReportingData reportingData(String rail) {
        return reporting(rail);
    }

    @Transactional(readOnly = true)
    public AccountLedgerResult accountLedgerRecords(String subscriberRef, String advanceId, String marketCode) {
        LoanLookup lookup = lookup(subscriberRef);
        List<UUID> advanceFilter = null;
        if (advanceId != null && !advanceId.isBlank()) {
            advanceFilter = List.of(UUID.fromString(advanceId.trim()));
        } else if (subscriberRef != null && !subscriberRef.isBlank()) {
            if (lookup.context() == null || lookup.context().advances() == null) {
                advanceFilter = List.of();
            } else {
                advanceFilter = lookup.context().advances().stream().map(LoanMgmtClient.LoanAdvance::advanceId).toList();
            }
        }
        List<AccountLedgerRecord> records = journalService.find(advanceFilter, marketCode).stream()
                .limit(CAP)
                .map(LedgerReportService::ledgerRecord)
                .toList();
        LoanContextView context = lookup.context() == null ? null : loanContext(lookup.context());
        return new AccountLedgerResult(records, context, lookup.status());
    }

    private ReportingData reporting(String rail) {
        List<ReconWindowRecord> windows = reconWindowRepository.findAllByOrderByWindowStartAsc().stream()
                .limit(CAP)
                .map(LedgerReportService::window)
                .toList();
        List<BilledVsCollected> billedRows = (rail == null || rail.isBlank())
                ? billedRepository.findAllByOrderByIdBusinessDateAsc()
                : billedRepository.findByIdRailOrderByIdBusinessDateAsc(rail.trim());
        List<BilledRecord> billed = billedRows.stream().limit(CAP).map(LedgerReportService::billed).toList();
        List<AuditRecord> audit = auditChainRepository.findAllByOrderBySeqAsc().stream().limit(CAP).map(LedgerReportService::audit).toList();
        List<WatermarkRecord> watermarks = watermarkRepository.findAllByOrderByIdProjectionNameAsc().stream()
                .limit(CAP)
                .map(LedgerReportService::watermark)
                .toList();
        return new ReportingData(windows, billed, audit, watermarks);
    }

    private LoanLookup lookup(String subscriberRef) {
        if (subscriberRef == null || subscriberRef.isBlank()) {
            return new LoanLookup(null, "NOT_REQUESTED");
        }
        if (!loanMgmtClient.configured()) {
            return new LoanLookup(null, "LOAN_MGMT_URL_UNSET");
        }
        try {
            return new LoanLookup(loanMgmtClient.context(subscriberRef.trim()), "OK");
        } catch (RuntimeException exception) {
            return new LoanLookup(null, "UNREACHABLE");
        }
    }

    private static AccountLedgerRecord ledgerRecord(JournalService.JournalView journal) {
        List<AccountLedgerLine> lines = journal.lines().stream()
                .map(line -> new AccountLedgerLine(line.lineId().toString(), line.accountCode(), line.side(), line.amount()))
                .toList();
        return new AccountLedgerRecord(journal.journalId().toString(), journal.sourceEventId(), journal.advanceId().toString(),
                journal.journalType(), journal.marketCode(), journal.currency(), journal.postedAt(), lines);
    }

    private static ReconWindowRecord window(ReconWindow row) {
        return new ReconWindowRecord(row.getWindowId().toString(), row.getWindowStart().toString(), row.getWindowEnd().toString(),
                Support.money(row.getQuotedTotal()).toPlainString(), Support.money(row.getIngestedTotal()).toPlainString(),
                Support.money(row.getPostedTotal()).toPlainString(), Long.toString(row.getFilteredCount()), row.getBreakCount(), row.getExceptionOwner());
    }

    private static BilledRecord billed(BilledVsCollected row) {
        return new BilledRecord(row.getId().getRail(), row.getId().getBusinessDate().toString(),
                Support.money(row.getBilled()).toPlainString(), Support.money(row.getCollected()).toPlainString());
    }

    private static AuditRecord audit(AuditChain row) {
        HexFormat hex = HexFormat.of();
        return new AuditRecord(Long.toString(row.getSeq()), row.getEntityType(), row.getEntityId(), row.getAction(),
                hex.formatHex(row.getPayloadHash()), hex.formatHex(row.getPrevHash()), hex.formatHex(row.getChainHash()),
                row.getCreatedAt().toString());
    }

    private static WatermarkRecord watermark(ProjectionWatermark row) {
        return new WatermarkRecord(row.getId().getProjectionName(), row.getId().getTopic(), row.getId().getPartitionNo(),
                Long.toString(row.getLastOffset()), row.getUpdatedAt().toString());
    }

    private static LoanContextView loanContext(LoanMgmtClient.LoanContext context) {
        List<LoanAdvanceView> advances = context.advances() == null ? List.of() : context.advances().stream()
                .map(advance -> new LoanAdvanceView(
                        advance.advanceId().toString(),
                        advance.status(),
                        advance.currency(),
                        advance.dueDate() == null ? null : advance.dueDate().toString(),
                        advance.principalResidual() == null ? null : Support.money(advance.principalResidual()).toPlainString(),
                        advance.feeResidual() == null ? null : Support.money(advance.feeResidual()).toPlainString(),
                        advance.allocations() == null ? List.of() : advance.allocations().stream()
                                .map(row -> new LoanAllocationView(row.component(), row.seq(),
                                        row.amount() == null ? null : Support.money(row.amount()).toPlainString()))
                                .toList()))
                .toList();
        return new LoanContextView(context.subscriberRef(), advances);
    }

    private record LoanLookup(LoanMgmtClient.LoanContext context, String status) {
    }

    public record AccountLedgerResult(List<AccountLedgerRecord> records, LoanContextView loanContext,
                                      String loanContextStatus) {
    }

    public record AccountLedgerRecord(String journalId, String sourceEventId, String advanceId, String journalType,
                                      String marketCode, String currency, String postedAt, List<AccountLedgerLine> lines) {
    }

    public record AccountLedgerLine(String lineId, String accountCode, String side, String amount) {
    }

    public record ReportingData(List<ReconWindowRecord> reconWindows, List<BilledRecord> billedVsCollected,
                                List<AuditRecord> auditChain, List<WatermarkRecord> watermarks) {
    }

    public record ReconWindowRecord(String windowId, String windowStart, String windowEnd, String quotedTotal,
                                    String ingestedTotal, String postedTotal, String filteredCount, int breakCount,
                                    String exceptionOwner) {
    }

    public record BilledRecord(String rail, String businessDate, String billed, String collected) {
    }

    public record AuditRecord(String seq, String entityType, String entityId, String action, String payloadHash,
                              String prevHash, String chainHash, String createdAt) {
    }

    public record WatermarkRecord(String projectionName, String topic, int partitionNo, String lastOffset, String updatedAt) {
    }

    public record LoanContextView(String subscriberRef, List<LoanAdvanceView> advances) {
    }

    public record LoanAdvanceView(String advanceId, String status, String currency, String dueDate,
                                  String principalResidual, String feeResidual, List<LoanAllocationView> allocations) {
    }

    public record LoanAllocationView(String component, Integer seq, String amount) {
    }
}
