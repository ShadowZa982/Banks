package org.kalistudio.kBank.loan;

public class LoanData {
    public double borrowed;
    public double totalToPay;
    public long timeLeft; // tính bằng giây

    public LoanData(double borrowed, double totalToPay, long timeLeft) {
        this.borrowed = borrowed;
        this.totalToPay = totalToPay;
        this.timeLeft = timeLeft;
    }
}