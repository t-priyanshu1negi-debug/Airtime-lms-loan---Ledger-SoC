package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class WatermarkKey implements Serializable {

    @Column(name = "projection_name", nullable = false, length = 64)
    private String projectionName;

    @Column(name = "topic", nullable = false, length = 64)
    private String topic;

    @Column(name = "partition_no", nullable = false)
    private Integer partitionNo;

    public WatermarkKey() {
    }

    public WatermarkKey(String projectionName, String topic, Integer partitionNo) {
        this.projectionName = projectionName;
        this.topic = topic;
        this.partitionNo = partitionNo;
    }

    public String getProjectionName() { return projectionName; }
    public String getTopic() { return topic; }
    public Integer getPartitionNo() { return partitionNo; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WatermarkKey that)) return false;
        return Objects.equals(projectionName, that.projectionName)
                && Objects.equals(topic, that.topic)
                && Objects.equals(partitionNo, that.partitionNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectionName, topic, partitionNo);
    }
}
