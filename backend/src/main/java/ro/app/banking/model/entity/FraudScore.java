package ro.app.banking.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"FRAUD_SCORE\"")
public class FraudScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "amount_risk", precision = 5, scale = 4)
    private BigDecimal amountRisk;

    @Column(name = "time_risk", precision = 5, scale = 4)
    private BigDecimal timeRisk;

    @Column(name = "category_risk", precision = 5, scale = 4)
    private BigDecimal categoryRisk;

    @Column(name = "frequency_risk", precision = 5, scale = 4)
    private BigDecimal frequencyRisk;

    @Column(name = "device_risk", precision = 5, scale = 4)
    private BigDecimal deviceRisk;

    @Column(name = "reasons", columnDefinition = "TEXT[]")
    private String[] reasons;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FraudScore() {
    }

    public FraudScore(Transaction transaction, BigDecimal score, String riskLevel) {
        this.transaction = transaction;
        this.score = score;
        this.riskLevel = riskLevel;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getAmountRisk() {
        return amountRisk;
    }

    public void setAmountRisk(BigDecimal amountRisk) {
        this.amountRisk = amountRisk;
    }

    public BigDecimal getTimeRisk() {
        return timeRisk;
    }

    public void setTimeRisk(BigDecimal timeRisk) {
        this.timeRisk = timeRisk;
    }

    public BigDecimal getCategoryRisk() {
        return categoryRisk;
    }

    public void setCategoryRisk(BigDecimal categoryRisk) {
        this.categoryRisk = categoryRisk;
    }

    public BigDecimal getFrequencyRisk() {
        return frequencyRisk;
    }

    public void setFrequencyRisk(BigDecimal frequencyRisk) {
        this.frequencyRisk = frequencyRisk;
    }

    public BigDecimal getDeviceRisk() {
        return deviceRisk;
    }

    public void setDeviceRisk(BigDecimal deviceRisk) {
        this.deviceRisk = deviceRisk;
    }

    public String[] getReasons() {
        return reasons;
    }

    public void setReasons(String[] reasons) {
        this.reasons = reasons;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
