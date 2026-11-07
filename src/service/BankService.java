package service;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import repository.Repository;
import util.ReportGenerator;
import util.StatePersister;
import util.StatementFormatter;
import util.TransactionLog;
import util.Validator;

public class BankService implements AutoCloseable {
    private final Repository<Account> accounts;
    private final TransactionProcessor transactionProcessor;
    private final Path stateFile;
    private final Path logFile;

    public BankService(
            Repository<Account> accounts,
            TransactionProcessor transactionProcessor,
            Path stateFile,
            Path logFile) throws IOException, ClassNotFoundException {
        this.accounts = accounts;
        this.transactionProcessor = transactionProcessor;
        this.stateFile = stateFile;
        this.logFile = logFile;

        if (Files.exists(stateFile)) {
            for (Account account : StatePersister.load(stateFile)) {
                accounts.save(account);
                transactionProcessor.addAccount(account);
            }
        }
    }

    public Account openAccount(String ownerName, String email, String mobile, String type, long initialBalance)
            throws BankException, IOException {
        if (ownerName == null || ownerName.isBlank()) {
            throw new BankException("Owner name cannot be empty.");
        }
        if (!Validator.isValidEmail(email)) {
            throw new BankException("Please enter a valid email address.");
        }
        if (!Validator.isValidMobile(mobile)) {
            throw new BankException("Please enter a valid 10-digit mobile number.");
        }
        if (initialBalance <= 0) {
            throw new InvalidAmountException("Opening balance must be greater than zero.");
        }
        if (type == null) {
            throw new BankException("Account type is required.");
        }

        Account account = switch (type.trim().toUpperCase()) {
            case "SAVINGS" -> new SavingsAccount(ownerName.trim(), initialBalance, 0);
            case "CURRENT" -> new CurrentAccount(ownerName.trim(), initialBalance, 0);
            case "FIXED", "FIXED_DEPOSIT" -> new FixedDepositAccount(ownerName.trim(), initialBalance);
            default -> throw new BankException("Choose SAVINGS, CURRENT, or FIXED account type.");
        };
        accounts.save(account);
        transactionProcessor.addAccount(account);
        TransactionLog.append(logFile, "DEPOSIT," + initialBalance);
        return account;
    }

    public void deposit(String accountNumber, long amount) throws BankException, IOException {
        Account account = findAccount(accountNumber);
        account.deposit(amount);
        TransactionLog.append(logFile, "DEPOSIT," + amount);
    }

    public boolean withdraw(String accountNumber, long amount) throws BankException, IOException {
        Account account = findAccount(accountNumber);
        boolean withdrawn = account.withdraw(amount);
        TransactionLog.append(logFile, "WITHDRAWAL," + amount);
        return withdrawn;
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, long amount)
            throws BankException, IOException {
        Account from = findAccount(fromAccountNumber);
        Account to = findAccount(toAccountNumber);
        from.transfer(to, amount);
        TransactionLog.append(logFile, "WITHDRAWAL," + amount);
        TransactionLog.append(logFile, "DEPOSIT," + amount);
    }

    public String statement(String accountNumber) throws AccountNotFoundException {
        return StatementFormatter.buildStatement(findAccount(accountNumber));
    }

    public ArrayList<Account> listAccounts() {
        ArrayList<Account> result = accounts.findAll();
        result.sort(Account::compareTo);
        return result;
    }

    public void processBatch(List<TransactionRequest> transactions) throws BankException, IOException {
        CountDownLatch completion = new CountDownLatch(transactions.size());
        ConcurrentLinkedQueue<Exception> failures = new ConcurrentLinkedQueue<>();

        for (TransactionRequest transaction : transactions) {
            if (transactionProcessor.getAccount(transaction.accountNumber()) == null) {
                throw new AccountNotFoundException("Account " + transaction.accountNumber() + " was not found.");
            }
        }

        for (TransactionRequest transaction : transactions) {
            transactionProcessor.submit(() -> {
                try {
                    Account account = transactionProcessor.getAccount(transaction.accountNumber());
                    if (account == null) {
                        throw new AccountNotFoundException(
                                "Account " + transaction.accountNumber() + " was not found.");
                    }
                    if (transaction.type() == TransactionType.DEPOSIT) {
                        account.deposit(transaction.amount());
                        TransactionLog.append(logFile, "DEPOSIT," + transaction.amount());
                    } else {
                        account.withdraw(transaction.amount());
                        TransactionLog.append(logFile, "WITHDRAWAL," + transaction.amount());
                    }
                } catch (BankException | IOException e) {
                    failures.add(e);
                } catch (RuntimeException e) {
                    failures.add(e);
                } finally {
                    completion.countDown();
                }
            });
        }

        try {
            completion.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BankException("Interrupted while waiting for batch transactions.", e);
        }

        Exception failure = failures.peek();
        if (failure instanceof BankException bankException) {
            throw bankException;
        }
        if (failure instanceof IOException ioException) {
            throw ioException;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
    }

    public String generateReport(Path logsDirectory, Path reportFile) throws IOException {
        return ReportGenerator.generate(logsDirectory, reportFile);
    }

    @Override
    public void close() throws IOException {
        transactionProcessor.stop();
        StatePersister.save(accounts.findAll().toArray(Account[]::new), stateFile);
    }

    private Account findAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.findById(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account " + accountNumber + " was not found.");
        }
        return account;
    }

    public record TransactionRequest(TransactionType type, String accountNumber, long amount) {
        public TransactionRequest {
            if (type == null || accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("Transaction type and account number are required.");
            }
        }
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }
}
