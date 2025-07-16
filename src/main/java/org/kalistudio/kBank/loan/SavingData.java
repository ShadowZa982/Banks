package org.kalistudio.kBank.loan;

import java.util.UUID;

public class SavingData {
    private final UUID uuid;
    private final double amount;
    private final int months;
    private final long createdAt;
    private final long matureAt;
    private final double rate;

    public SavingData(UUID uuid, double amount, int months, long createdAt, long matureAt, double rate) {
        this.uuid = uuid;
        this.amount = amount;
        this.months = months;
        this.createdAt = createdAt;
        this.matureAt = matureAt;
        this.rate = rate;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getAmount() {
        return amount;
    }

    public int getMonths() {
        return months;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getMatureAt() {
        return matureAt;
    }

    public double getRate() {
        return rate;
    }
}