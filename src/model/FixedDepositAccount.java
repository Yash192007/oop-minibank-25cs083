package model;

import java.io.Serializable;

public class FixedDepositAccount extends Account implements Premium, Serializable {
    private static final long serialVersionUID = 1L;

    public FixedDepositAccount(String ownerName, long balance) {
        super(ownerName, balance);
    }

    @Override
    public double interestRate() {
        return 7.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return false;
    }

    @Override
    protected long withdrawalShortfall(long amount) {
        return amount;
    }
}