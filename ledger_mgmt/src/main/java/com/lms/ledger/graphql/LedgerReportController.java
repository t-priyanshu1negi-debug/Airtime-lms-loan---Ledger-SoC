package com.lms.ledger.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class LedgerReportController {

    private final LedgerReportService ledgerReportService;

    public LedgerReportController(LedgerReportService ledgerReportService) {
        this.ledgerReportService = ledgerReportService;
    }

    @QueryMapping
    public LedgerReportService.ReportingData reportingData(@Argument String rail) {
        return ledgerReportService.reportingData(rail);
    }

    @QueryMapping
    public LedgerReportService.AccountLedgerResult accountLedgerRecords(@Argument String subscriberRef,
                                                                        @Argument String advanceId,
                                                                        @Argument String marketCode) {
        return ledgerReportService.accountLedgerRecords(subscriberRef, advanceId, marketCode);
    }
}
