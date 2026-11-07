package service;

import exception.BankException;
import model.Account;

public class AccountTransaction implements Runnable {
    private final Account account;
    private final long amount;
    private final boolean deposit;

    public AccountTransaction(Account account, long amount, boolean deposit) {
        this.account = account;
        this.amount = amount;
        this.deposit = deposit;
    }

    @Override
    public void run() {
        try {
            if (deposit) {
                account.deposit(amount);
            } else {
                account.withdraw(amount);
            }
        } catch (BankException e) {
            System.out.println("Transaction declined: " + e.getMessage());
        }
    }
}
