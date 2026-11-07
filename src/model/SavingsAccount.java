package model;

import java.io.Serializable;

public class SavingsAccount extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private long minBalance;

    public SavingsAccount(String ownerName, long balance, long minBalance) {
        super(ownerName, balance);
        this.minBalance = minBalance;
    }

    @Override
    public double interestRate() {
        return 4.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return (this.balance - amount) >= minBalance;
    }

    @Override
    protected long withdrawalShortfall(long amount) {
        long available = Math.max(0, balance - minBalance);
        return amount - available;
    }

    public long getMinBalance() {
        return minBalance;
    }
}