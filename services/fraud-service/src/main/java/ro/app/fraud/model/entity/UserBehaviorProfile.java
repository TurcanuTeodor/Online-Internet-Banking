package ro.app.fraud.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "USER_BEHAVIOR_PROFILE", schema = "fraud")
public class UserBehaviorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CLIENT_ID", nullable = false, unique = true)
    private Long clientId;

    @Column(name = "AVG_TRANSACTION_AMOUNT", nullable = false)
    private double avgTransactionAmount;

    @Column(name = "MAX_TRANSACTION_AMOUNT", nullable = false)
    private double maxTransactionAmount;

    @Column(name = "TRANSACTION_COUNT", nullable = false)
    private long transactionCount;

    @Column(name = "AVG_DAILY_TRANSACTIONS", nullable = false)
    private double avgDailyTransactions;

    @Column(name = "TYPICAL_HOUR_START", nullable = false)
    private int typicalHourStart = 8;

    @Column(name = "TYPICAL_HOUR_END", nullable = false)
    private int typicalHourEnd = 22;

    @Column(name = "COMMON_IBANS")
    private String commonIbans;

    @Column(name = "LAST_UPDATED", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public double getAvgTransactionAmount() { return avgTransactionAmount; }
    public void setAvgTransactionAmount(double avgTransactionAmount) { this.avgTransactionAmount = avgTransactionAmount; }

    public double getMaxTransactionAmount() { return maxTransactionAmount; }
    public void setMaxTransactionAmount(double maxTransactionAmount) { this.maxTransactionAmount = maxTransactionAmount; }

    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }

    public double getAvgDailyTransactions() { return avgDailyTransactions; }
    public void setAvgDailyTransactions(double avgDailyTransactions) { this.avgDailyTransactions = avgDailyTransactions; }

    public int getTypicalHourStart() { return typicalHourStart; }
    public void setTypicalHourStart(int typicalHourStart) { this.typicalHourStart = typicalHourStart; }

    public int getTypicalHourEnd() { return typicalHourEnd; }
    public void setTypicalHourEnd(int typicalHourEnd) { this.typicalHourEnd = typicalHourEnd; }

    public String getCommonIbans() { return commonIbans; }
    public void setCommonIbans(String commonIbans) { this.commonIbans = commonIbans; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
