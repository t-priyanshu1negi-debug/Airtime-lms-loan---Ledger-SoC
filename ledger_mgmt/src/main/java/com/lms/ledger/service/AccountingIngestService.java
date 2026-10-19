package com.lms.ledger.service;

import com.lms.ledger.client.LoanMgmtClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountingIngestService {

    private final LoanMgmtClient loanMgmtClient;
    private final JournalService journalService;
    private final ReportingCommandService reportingCommandService;

    public AccountingIngestService(LoanMgmtClient loanMgmtClient,
                                   JournalService journalService,
                                   ReportingCommandService reportingCommandService) {
        this.loanMgmtClient = loanMgmtClient;
        this.journalService = journalService;
        this.reportingCommandService = reportingCommandService;
    }

    public IngestResult ingest() {
        List<LoanMgmtClient.OutboxEvent> events = loanMgmtClient.unpublishedAccounting();
        int booked = 0;
        int alreadyBooked = 0;
        int skipped = 0;
        for (LoanMgmtClient.OutboxEvent event : events) {
            JournalService.JournalResult result = journalService.bookAccountingEvent(event);
            if (result.journal() == null) {
                skipped++;
            } else if (result.created()) {
                booked++;
            } else {
                alreadyBooked++;
            }
            reportingCommandService.noteIngest(event);
            loanMgmtClient.markPublished(event.outboxId());
        }
        return new IngestResult(events.size(), booked, alreadyBooked, skipped);
    }

    public record IngestResult(int examined, int booked, int alreadyBooked, int skipped) {
    }
}
