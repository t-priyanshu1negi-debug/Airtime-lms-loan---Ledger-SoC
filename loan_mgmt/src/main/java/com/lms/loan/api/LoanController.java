package com.lms.loan.api;

import com.lms.loan.api.dto.LoanDtos.AcknowledgeRequest;
import com.lms.loan.api.dto.LoanDtos.AdvanceView;
import com.lms.loan.api.dto.LoanDtos.CancelRequest;
import com.lms.loan.api.dto.LoanDtos.CreateAdvanceRequest;
import com.lms.loan.api.dto.LoanDtos.CreateDisclosureRequest;
import com.lms.loan.api.dto.LoanDtos.CreateMandateRequest;
import com.lms.loan.api.dto.LoanDtos.CreditResultRequest;
import com.lms.loan.api.dto.LoanDtos.DisclosureView;
import com.lms.loan.api.dto.LoanDtos.DisputeView;
import com.lms.loan.api.dto.LoanDtos.DunningTickRequest;
import com.lms.loan.api.dto.LoanDtos.DunningTickView;
import com.lms.loan.api.dto.LoanDtos.MandateView;
import com.lms.loan.api.dto.LoanDtos.OutboxView;
import com.lms.loan.api.dto.LoanDtos.QuoteRequest;
import com.lms.loan.api.dto.LoanDtos.QuoteView;
import com.lms.loan.api.dto.LoanDtos.RaiseDisputeRequest;
import com.lms.loan.api.dto.LoanDtos.ReleaseRequest;
import com.lms.loan.api.dto.LoanDtos.ScheduleView;
import com.lms.loan.api.dto.LoanDtos.SettleRequest;
import com.lms.loan.api.dto.LoanDtos.SettlementView;
import com.lms.loan.api.dto.LoanDtos.SignalView;
import com.lms.loan.api.dto.LoanDtos.SubscriberContext;
import com.lms.loan.api.dto.LoanDtos.TransitionDisputeRequest;
import com.lms.loan.api.dto.LoanDtos.VoluntaryPaymentRequest;
import com.lms.loan.service.DisputeService;
import com.lms.loan.service.DunningService;
import com.lms.loan.service.LoanQueryService;
import com.lms.loan.service.OriginationService;
import com.lms.loan.service.OutboxWriter;
import com.lms.loan.service.RecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LoanController {

    private final OriginationService originationService;
    private final RecoveryService recoveryService;
    private final DisputeService disputeService;
    private final DunningService dunningService;
    private final LoanQueryService loanQueryService;

    public LoanController(OriginationService originationService,
                          RecoveryService recoveryService,
                          DisputeService disputeService,
                          DunningService dunningService,
                          LoanQueryService loanQueryService) {
        this.originationService = originationService;
        this.recoveryService = recoveryService;
        this.disputeService = disputeService;
        this.dunningService = dunningService;
        this.loanQueryService = loanQueryService;
    }

    @PostMapping("/disclosures")
    public ResponseEntity<DisclosureView> createDisclosure(@Valid @RequestBody CreateDisclosureRequest request) {
        WriteResult<DisclosureView> result = originationService.createDisclosure(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.body());
    }

    @PostMapping("/disclosures/acknowledge")
    public DisclosureView acknowledge(@Valid @RequestBody AcknowledgeRequest request) {
        return originationService.acknowledge(request);
    }

    @GetMapping("/disclosures/{disclosureId}")
    public DisclosureView disclosure(@PathVariable UUID disclosureId) {
        return originationService.getDisclosure(disclosureId);
    }

    @PostMapping("/mandates")
    public ResponseEntity<MandateView> armMandate(@Valid @RequestBody CreateMandateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(originationService.armMandate(request));
    }

    @PostMapping("/mandates/{mandateId}/revoke")
    public MandateView revoke(@PathVariable UUID mandateId) {
        return originationService.revoke(mandateId);
    }

    @GetMapping("/subscribers/{subscriberRef}/mandates")
    public List<MandateView> mandates(@PathVariable String subscriberRef) {
        return loanQueryService.mandates(subscriberRef);
    }

    @PostMapping("/advances")
    public ResponseEntity<AdvanceView> createAdvance(@Valid @RequestBody CreateAdvanceRequest request) {
        WriteResult<AdvanceView> result = originationService.createAdvance(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.body());
    }

    @GetMapping("/advances/{advanceId}")
    public AdvanceView advance(@PathVariable UUID advanceId) {
        return loanQueryService.advance(advanceId);
    }

    @GetMapping("/subscribers/{subscriberRef}/advances")
    public List<AdvanceView> advances(@PathVariable String subscriberRef) {
        return loanQueryService.advances(subscriberRef);
    }

    @GetMapping("/advances/{advanceId}/schedule")
    public List<ScheduleView> schedule(@PathVariable UUID advanceId) {
        return loanQueryService.schedule(advanceId);
    }

    @PostMapping("/advances/{advanceId}/credit-result")
    public AdvanceView creditResult(@PathVariable UUID advanceId, @Valid @RequestBody CreditResultRequest request) {
        return originationService.creditResult(advanceId, request);
    }

    @PostMapping("/advances/{advanceId}/cancel")
    public AdvanceView cancel(@PathVariable UUID advanceId, @Valid @RequestBody CancelRequest request) {
        return originationService.cancel(advanceId, request);
    }

    @PostMapping("/advances/{advanceId}/disputes")
    public ResponseEntity<DisputeView> raiseDispute(@PathVariable UUID advanceId,
                                                    @Valid @RequestBody RaiseDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disputeService.raise(advanceId, request));
    }

    @GetMapping("/advances/{advanceId}/disputes")
    public List<DisputeView> disputes(@PathVariable UUID advanceId) {
        return disputeService.list(advanceId);
    }

    @PostMapping("/disputes/{disputeId}/transition")
    public DisputeView transition(@PathVariable UUID disputeId, @Valid @RequestBody TransitionDisputeRequest request) {
        return disputeService.transition(disputeId, request);
    }

    @PostMapping("/recovery/quote")
    public QuoteView quote(@Valid @RequestBody QuoteRequest request) {
        return recoveryService.quote(request);
    }

    @PostMapping("/recovery/settle")
    public SettlementView settle(@Valid @RequestBody SettleRequest request,
                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return recoveryService.settle(request, idempotencyKey);
    }

    @PostMapping("/recovery/release")
    public QuoteView release(@Valid @RequestBody ReleaseRequest request) {
        return recoveryService.release(request);
    }

    @GetMapping("/recovery/signals/{signalId}")
    public SignalView signal(@PathVariable String signalId) {
        return recoveryService.signal(signalId);
    }

    @PostMapping("/payments")
    public SettlementView pay(@Valid @RequestBody VoluntaryPaymentRequest request,
                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return recoveryService.voluntary(request, idempotencyKey);
    }

    @PostMapping("/dunning/tick")
    public DunningTickView tick(@Valid @RequestBody DunningTickRequest request) {
        return dunningService.tick(request);
    }

    @GetMapping("/internal/subscribers/{subscriberRef}/context")
    public SubscriberContext context(@PathVariable String subscriberRef) {
        return loanQueryService.context(subscriberRef);
    }

    @GetMapping("/internal/outbox")
    public List<OutboxView> unpublished(@RequestParam(defaultValue = OutboxWriter.ACCOUNTING_EVENTS) String topic) {
        return loanQueryService.unpublished(topic);
    }

    @PostMapping("/internal/outbox/{outboxId}/published")
    public ResponseEntity<Void> published(@PathVariable long outboxId) {
        loanQueryService.markPublished(outboxId);
        return ResponseEntity.noContent().build();
    }
}
