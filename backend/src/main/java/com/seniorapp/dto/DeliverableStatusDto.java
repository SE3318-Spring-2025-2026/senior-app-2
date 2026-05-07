package com.seniorapp.dto;

public class DeliverableStatusDto {
    private Long deliverableId;
    private String status;
    private Double score;

    public DeliverableStatusDto(Long deliverableId, String status, Double score) {
        this.deliverableId = deliverableId;
        this.status = status;
        this.score = score;
    }

    public Long getDeliverableId() { return deliverableId; }
    public String getStatus() { return status; }
    public Double getScore() { return score; }
}
