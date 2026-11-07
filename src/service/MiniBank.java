package service;

import exception.BankException;
import exception.InsufficientFundsException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import model.Account;
import repository.Repository;

public class MiniBank {
    private static final Path STATE_FILE = Paths.get("data", "accounts.ser");
    private static final Path LOGS_DIRECTORY = Paths.get("logs");
    private static final Path LOG_FILE = LOGS_DIRECTORY.resolve("transactions.log");
    private static final Path REPORT_FILE = Paths.get("reports", "end-of-day.txt");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BankService bankService;
        try {
            bankService = new BankService(
                    new Repository<>(),
                    new TransactionProcessor(),
                    STATE_FILE,
                    LOG_FILE);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not load saved accounts: " + e.getMessage());
            scanner.close();
            return;
        }

        System.out.println("Welcome to National MiniBank.");
        System.out.println("Loaded " + bankService.listAccounts().size() + " saved account(s).");
        boolean running = true;
        try {
            while (running) {
                printMenu();
                String choice = readLine(scanner, "Enter your choice: ");
                if (choice == null) {
                    break;
                }

                try {
                    switch (choice.trim()) {
                        case "1" -> openAccount(scanner, bankService);
                        case "2" -> deposit(scanner, bankService);
                        case "3" -> withdraw(scanner, bankService);
                        case "4" -> transfer(scanner, bankService);
                        case "5" -> printStatement(scanner, bankService);
                        case "6" -> listAccounts(bankService);
                        case "7" -> processBatch(scanner, bankService);
                        case "8" -> printReport(bankService);
                        case "9" -> running = false;
                        default -> System.out.println("Choose an option from 1 to 9.");
                    }
                } catch (BankException | IOException e) {
                    System.out.println("Operation failed: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid input: " + e.getMessage());
                }
            }
        } finally {
            try {
                bankService.close();
                System.out.println("Accounts saved. Thank you for using MiniBank.");
            } catch (IOException | IllegalStateException e) {
                System.out.println("Could not safely close MiniBank: " + e.getMessage());
            }
            scanner.close();
        }
    }

    private static void printMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Open account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Print statement");
        System.out.println("6. List accounts");
        System.out.println("7. Process transaction batch");
        System.out.println("8. Generate end-of-day report");
        System.out.println("9. Save and exit");
    }

    private static void openAccount(Scanner scanner, BankService bankService)
            throws BankException, IOException {
        String owner = requiredLine(scanner, "Owner name: ");
        String email = requiredLine(scanner, "Email: ");
        String mobile = requiredLine(scanner, "Mobile: ");
        String type = requiredLine(scanner, "Account type (SAVINGS/CURRENT/FIXED): ");
        long openingBalance = readAmount(scanner, "Opening balance: ");
        Account account = bankService.openAccount(owner, email, mobile, type, openingBalance);
        System.out.println("Account opened: " + account);
    }

    private static void deposit(Scanner scanner, BankService bankService) throws BankException, IOException {
        String accountNumber = requiredLine(scanner, "Account number: ");
        long amount = readAmount(scanner, "Deposit amount: ");
        bankService.deposit(accountNumber, amount);
        System.out.println("Deposit completed.");
    }

    private static void withdraw(Scanner scanner, BankService bankService) throws BankException, IOException {
        String accountNumber = requiredLine(scanner, "Account number: ");
        long amount = readAmount(scanner, "Withdrawal amount: ");
        try {
            bankService.withdraw(accountNumber, amount);
            System.out.println("Withdrawal completed.");
        } catch (InsufficientFundsException e) {
            System.out.println("Withdrawal declined. Shortfall: " + e.getShortfall() + ".");
        }
    }

    private static void transfer(Scanner scanner, BankService bankService) throws BankException, IOException {
        String from = requiredLine(scanner, "From account number: ");
        String to = requiredLine(scanner, "To account number: ");
        long amount = readAmount(scanner, "Transfer amount: ");
        bankService.transfer(from, to, amount);
        System.out.println("Transfer completed.");
    }

    private static void printStatement(Scanner scanner, BankService bankService) throws BankException {
        String accountNumber = requiredLine(scanner, "Account number: ");
        System.out.println(bankService.statement(accountNumber));
    }

    private static void listAccounts(BankService bankService) {
        ArrayList<Account> accounts = bankService.listAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts are available.");
            return;
        }
        for (Account account : accounts) {
            System.out.println(account);
        }
    }

    private static void processBatch(Scanner scanner, BankService bankService)
            throws BankException, IOException {
        int count = readCount(scanner, "Number of transactions (1-1000): ", 1, 1000);
        ArrayList<BankService.TransactionRequest> transactions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String type = requiredLine(scanner, "Transaction " + (i + 1) + " type (DEPOSIT/WITHDRAWAL): ");
            BankService.TransactionType transactionType;
            try {
                transactionType = BankService.TransactionType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Transaction type must be DEPOSIT or WITHDRAWAL.");
            }
            String accountNumber = requiredLine(scanner, "Account number: ");
            long amount = readAmount(scanner, "Amount: ");
            transactions.add(new BankService.TransactionRequest(transactionType, accountNumber, amount));
        }
        bankService.processBatch(transactions);
        System.out.println("All batch transactions completed.");
    }

    private static void printReport(BankService bankService) throws IOException {
        String report = bankService.generateReport(LOGS_DIRECTORY, REPORT_FILE);
        System.out.println(report);
        System.out.println("Report saved to " + REPORT_FILE + ".");
    }

    private static String requiredLine(Scanner scanner, String prompt) {
        String line = readLine(scanner, prompt);
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("A value is required.");
        }
        return line.trim();
    }

    private static long readAmount(Scanner scanner, String prompt) {
        String value = requiredLine(scanner, prompt);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a whole number.");
        }
    }

    private static int readCount(Scanner scanner, String prompt, int minimum, int maximum) {
        String value = requiredLine(scanner, prompt);
        try {
            int count = Integer.parseInt(value);
            if (count < minimum || count > maximum) {
                throw new IllegalArgumentException("Enter a number from " + minimum + " to " + maximum + ".");
            }
            return count;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a whole number.");
        }
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }
}
