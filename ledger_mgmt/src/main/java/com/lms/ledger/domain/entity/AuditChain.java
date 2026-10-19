package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_chain")
public class AuditChain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq", nullable = false)
    private Long seq;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "payload_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] payloadHash;

    @Column(name = "prev_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] prevHash;

    @Column(name = "chain_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] chainHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getSeq() { return seq; }
    public void setSeq(Long seq) { this.seq = seq; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public byte[] getPayloadHash() { return payloadHash; }
    public void setPayloadHash(byte[] payloadHash) { this.payloadHash = payloadHash; }
    public byte[] getPrevHash() { return prevHash; }
    public void setPrevHash(byte[] prevHash) { this.prevHash = prevHash; }
    public byte[] getChainHash() { return chainHash; }
    public void setChainHash(byte[] chainHash) { this.chainHash = chainHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
