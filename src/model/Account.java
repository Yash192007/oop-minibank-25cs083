package model;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import java.util.Objects;
import model.annotation.Id;
import model.annotation.Positive;

public abstract class Account implements Transactable, InterestBearing {
    private static long accountCounter = 0;
    private static final Object TRANSFER_TIE_LOCK = new Object();

    @Id
    private final String accountNumber;
    private String ownerName;
    @Positive
    protected long balance;
    private boolean active;

    private static String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    public Account(String ownerName, long balance) {
        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = balance;
        this.active = true;
    }

    public Account(String accountNumber, String ownerName, long balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.active = true;
    }

    public Account(String ownerName) {
        this(ownerName, 0);
    }

    @Override
    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    protected long withdrawalShortfall(long amount) {
        return Math.max(0, amount - Math.max(0, balance));
    }

    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero.");
        }
        this.balance += amount;
    }

    @Override
    public synchronized boolean withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero.");
        }
        if (!canWithdraw(amount)) {
            long shortfall = withdrawalShortfall(amount);
            throw new InsufficientFundsException(
                    "Insufficient funds: short by " + shortfall + ".", shortfall);
        }
        this.balance -= amount;
        return true;
    }

    public void transfer(Account to, long amount) throws BankException {
        try {
            if (to == null) {
                throw new AccountNotFoundException("Destination account was not found.");
            }
            int order = getAccountNumber().compareTo(to.getAccountNumber());
            if (order == 0) {
                order = Integer.compare(System.identityHashCode(this), System.identityHashCode(to));
            }
            if (order < 0) {
                transferWithLocks(this, to, to, amount);
            } else if (order > 0) {
                transferWithLocks(to, this, to, amount);
            } else {
                synchronized (TRANSFER_TIE_LOCK) {
                    transferWithLocks(this, to, to, amount);
                }
            }
        } catch (BankException e) {
            throw e;
        } finally {
            System.out.println("Transfer attempt finished.");
        }
    }

    private void transferWithLocks(Account firstLock, Account secondLock, Account destination, long amount)
            throws BankException {
        synchronized (firstLock) {
            synchronized (secondLock) {
                withdraw(amount);
                destination.deposit(amount);
            }
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public synchronized long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Account [Number=" + accountNumber + ", Owner=" + ownerName + ", Balance=" + balance + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }
}