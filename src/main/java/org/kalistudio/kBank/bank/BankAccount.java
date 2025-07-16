package org.kalistudio.kBank.bank;

import java.util.Date;

public class BankAccount {
    public final String ownerName;
    public final String accountNumber;
    public final Date createdAt;
    public double balance;

    public BankAccount(String ownerName, String accountNumber, Date createdAt, double balance) {
        this.ownerName = ownerName;
        this.accountNumber = accountNumber;
        this.createdAt = createdAt;
        this.balance = balance;
    }
}

