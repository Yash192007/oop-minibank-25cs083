package service;

import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.WithdrawRule;
import repository.Repository;
import util.AnnotationValidator;
import util.ReportGenerator;
import util.StatePersister;
import util.TransactionLog;
import static util.Validator.isValidEmail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

record BankInfo(String name, String branch) {}

enum MenuOption {
    OPEN_ACCOUNT,
    DEPOSIT,
    WITHDRAW,
    TRANSFER,
    EXIT;

    public static MenuOption fromChoice(int choice) {
        return switch (choice) {
            case 1 -> OPEN_ACCOUNT;
            case 2 -> DEPOSIT;
            case 3 -> WITHDRAW;
            case 4 -> TRANSFER;
            case 5 -> EXIT;
            default -> null;
        };
    }
}

public class MiniBank {

    public static void main(String[] args) {
        BankInfo bankInfo = new BankInfo("National MiniBank", "Main Branch");
        System.out.println("========================================");
        System.out.println("Welcome to " + bankInfo.name() + " (" + bankInfo.branch() + ")");
        System.out.println("========================================");
        System.out.println("Email validation demo: " + isValidEmail("customer@example.com"));

        Account[] accounts = new Account[3];
        accounts[0] = new SavingsAccount("Aarav Sharma", 10000, 2000);
        accounts[1] = new CurrentAccount("Priya Patel", 5000, 10000);
        accounts[2] = new FixedDepositAccount("Rohan Mehta", 50000);

        AccountStore accountStore = new AccountStore();
        for (Account account : accounts) {
            accountStore.add(account);
        }
        accountStore.addTransaction("DEPOSIT,AC0001,500");
        accountStore.addTransaction("WITHDRAWAL,AC0002,250");
        System.out.println("\n--- Accounts Sorted by Balance ---");
        for (Account account : accountStore.sortedByBalance()) {
            System.out.println(account);
        }

        Repository<Account> accountRepository = new Repository<>();
        accountRepository.saveAll(accounts);
        System.out.println("\n--- Generic Account Repository ---");
        Account foundAccount = accountRepository.findById(accounts[0].getAccountNumber());
        System.out.println("Found account: " + foundAccount);
        for (Account account : accountRepository.findAll()) {
            System.out.println(account);
        }
        System.out.println("Average sample amount: "
                + Repository.average(new Integer[] {100, 200, 300}));

        Account invalidAccount = new SavingsAccount("Invalid Customer", -100, 0);
        System.out.println("\n--- Annotation Validation Demo ---");
        for (String error : AnnotationValidator.validate(invalidAccount)) {
            System.out.println(error);
        }

        System.out.println("\n--- Polymorphism Demo (interestRate) ---");
        for (Account acc : accounts) {
            System.out.println(acc.getAccountNumber() + " | Owner: " + acc.getOwnerName() + " | Type: " + acc.getClass().getSimpleName() + " | Interest Rate: " + acc.interestRate() + "%");
        }

        runPersistenceAndReportingDemo(accounts);

        System.out.println("\n--- Testing canWithdraw & Polymorphic Withdrawals ---");
        WithdrawRule savingsRule = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account.canWithdraw(amount);
            }
        };
        WithdrawRule generalRule = (account, amount) -> account.canWithdraw(amount);
        System.out.println("Savings withdrawal rule (9000): " + savingsRule.allow(accounts[0], 9000));
        System.out.println("General withdrawal rule (12000): " + generalRule.allow(accounts[1], 12000));

        try {
            System.out.println("Savings Account withdrawal: " + accounts[0].withdraw(9000));
        } catch (InsufficientFundsException e) {
            System.out.println("Savings withdrawal declined: " + e.getMessage());
        } catch (InvalidAmountException e) {
            System.out.println("Savings withdrawal failed: " + e.getMessage());
        } finally {
            System.out.println("Savings withdrawal attempt completed.");
        }

        try {
            System.out.println("Current Account withdrawal: " + accounts[1].withdraw(12000));
        } catch (InsufficientFundsException e) {
            System.out.println("Current account withdrawal declined: " + e.getMessage());
        } catch (InvalidAmountException e) {
            System.out.println("Current account withdrawal failed: " + e.getMessage());
        } finally {
            System.out.println("Current account withdrawal attempt completed.");
        }

        try {
            System.out.println("Fixed Deposit Account withdrawal: " + accounts[2].withdraw(5000));
        } catch (InsufficientFundsException e) {
            System.out.println("Fixed deposit withdrawal declined: " + e.getMessage());
        } catch (InvalidAmountException e) {
            System.out.println("Fixed deposit withdrawal failed: " + e.getMessage());
        } finally {
            System.out.println("Fixed deposit withdrawal attempt completed.");
        }

        try {
            accounts[0].deposit(0);
        } catch (InvalidAmountException e) {
            System.out.println("Deposit declined: " + e.getMessage());
        }

        Account transferSource = new CurrentAccount("Transfer Sender", 2000, 0);
        Account transferDestination = new SavingsAccount("Transfer Recipient", 1000, 0);
        try {
            transferSource.transfer(transferDestination, 500);
            System.out.println("Transfer completed. New destination balance: " + transferDestination.getBalance());
        } catch (BankException e) {
            System.out.println("Transfer declined: " + e.getMessage());
        }

        try (BankSession session = new BankSession()) {
            System.out.println("Using a bank session resource.");
        }

        System.out.println("\n--- Synchronized Threaded Deposits ---");
        Account sharedAccount = new SavingsAccount("Concurrent Customer", 0, 0);
        int threadCount = 10;
        int depositsPerThread = 10_000;
        long depositAmount = 1;
        Thread[] depositThreads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            depositThreads[i] = new Thread(
                    new AccountWorker(sharedAccount, depositsPerThread, depositAmount),
                    "AccountWorker-" + (i + 1));
            depositThreads[i].start();
        }

        boolean interrupted = false;
        for (Thread depositThread : depositThreads) {
            boolean joined = false;
            while (!joined) {
                try {
                    depositThread.join();
                    joined = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                    System.out.println("Waiting for account workers to finish.");
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        long expectedBalance = (long) threadCount * depositsPerThread * depositAmount;
        System.out.println("Final balance: " + sharedAccount.getBalance()
                + " (expected " + expectedBalance + ")");

        runTransactionProcessorDemo();
        runProducerConsumerDemo();
        runOrderedTransferDemo();

        System.out.println("\n--- Pattern Matching instanceof Check ---");
        for (Account acc : accounts) {
            if (acc instanceof CurrentAccount ca) {
                System.out.println("Found Current Account: " + ca.getAccountNumber() + " with Overdraft Limit: " + ca.getOverdraftLimit());
            } else if (acc instanceof SavingsAccount sa) {
                System.out.println("Found Savings Account: " + sa.getAccountNumber() + " with Minimum Balance: " + sa.getMinBalance());
            } else if (acc instanceof FixedDepositAccount fda) {
                System.out.println("Found Fixed Deposit Account: " + fda.getAccountNumber() + " (Withdrawals Locked)");
            }
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Exit");
            System.out.print("Enter your choice (1-5): ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input! Please enter a number between 1 and 5.");
                scanner.next();
                continue;
            }

            int choice = scanner.nextInt();
            MenuOption option = MenuOption.fromChoice(choice);

            if (option == null) {
                System.out.println("Invalid choice. Please select an option from 1 to 5.");
                continue;
            }

            switch (option) {
                case OPEN_ACCOUNT -> System.out.println("[Open Account] — to be implemented in a later lab.");
                case DEPOSIT      -> System.out.println("[Deposit] — to be implemented in a later lab.");
                case WITHDRAW     -> System.out.println("[Withdraw] — to be implemented in a later lab.");
                case TRANSFER     -> System.out.println("[Transfer] — to be implemented in a later lab.");
                case EXIT         -> {
                    System.out.println("\nThank you for using " + bankInfo.name() + ". Goodbye!");
                    running = false;
                }
            }
        }

        scanner.close();
    }

    private static final class BankSession implements AutoCloseable {
        @Override
        public void close() {
            System.out.println("Bank session resource closed.");
        }
    }

    private static void runTransactionProcessorDemo() {
        System.out.println("\n--- Managed Transaction Processor ---");
        Account account = new SavingsAccount("Processor Customer", 10000, 0);
        TransactionProcessor processor = new TransactionProcessor();
        processor.addAccount(account);
        for (int i = 0; i < 50; i++) {
            processor.submit(new AccountTransaction(account, 10, true));
            processor.submit(new AccountTransaction(account, 5, false));
        }
        processor.stop();
        System.out.println("Processed 100 transactions. Final balance: " + account.getBalance());
    }

    private static void runProducerConsumerDemo() {
        System.out.println("\n--- Producer-Consumer Transaction Buffer ---");
        Account account = new SavingsAccount("Buffered Customer", 1000, 0);
        TransactionBuffer buffer = new TransactionBuffer(3);

        Thread consumer = new Thread(() -> {
            try {
                Runnable transaction;
                while ((transaction = buffer.take()) != null) {
                    transaction.run();
                    System.out.println("Consumer processed a transaction.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer was interrupted.");
            }
        }, "TransactionConsumer");

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    buffer.add(new AccountTransaction(account, 10, true));
                    buffer.add(new AccountTransaction(account, 5, false));
                    System.out.println("Producer added a transaction pair.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Producer was interrupted.");
            } finally {
                buffer.close();
            }
        }, "TransactionProducer");

        consumer.start();
        producer.start();
        joinUninterrupted(producer);
        joinUninterrupted(consumer);
        System.out.println("Producer-consumer final balance: " + account.getBalance());
    }

    private static void runOrderedTransferDemo() {
        System.out.println("\n--- Opposite-Direction Transfers with Ordered Locks ---");
        Account first = new SavingsAccount("Transfer A", 1000, 0);
        Account second = new SavingsAccount("Transfer B", 1000, 0);
        Thread forward = new Thread(() -> repeatTransfer(first, second), "Transfer-A-to-B");
        Thread reverse = new Thread(() -> repeatTransfer(second, first), "Transfer-B-to-A");
        forward.start();
        reverse.start();
        joinUninterrupted(forward);
        joinUninterrupted(reverse);
        System.out.println("Both transfer threads completed without deadlock.");
        System.out.println("Combined balance: " + (first.getBalance() + second.getBalance()));
    }

    private static void runPersistenceAndReportingDemo(Account[] accounts) {
        System.out.println("\n--- Persistence and End-of-Day Report ---");
        Path stateFile = Paths.get("data", "accounts.ser");
        Path logsDirectory = Paths.get("logs");
        Path transactionLog = logsDirectory.resolve("transactions.log");
        Path reportFile = Paths.get("reports", "end-of-day.txt");
        try {
            StatePersister.save(accounts, stateFile);
            Account[] restoredAccounts = StatePersister.load(stateFile);
            System.out.println("Reloaded accounts:");
            for (Account account : restoredAccounts) {
                System.out.println(account);
            }

            TransactionLog.append(transactionLog, "DEPOSIT,1500");
            TransactionLog.append(transactionLog, "WITHDRAWAL,250");
            String report = ReportGenerator.generate(logsDirectory, reportFile);
            System.out.println(report);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Persistence or report generation failed: " + e.getMessage());
        }
    }

    private static void repeatTransfer(Account from, Account to) {
        try {
            for (int i = 0; i < 5; i++) {
                from.transfer(to, 1);
            }
        } catch (BankException e) {
            System.out.println(Thread.currentThread().getName() + " transfer failed: " + e.getMessage());
        }
    }

    private static void joinUninterrupted(Thread thread) {
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                interrupted = true;
                System.out.println("Waiting for " + thread.getName() + " to finish.");
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}