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

        Account[] accounts = new Account[3];
        accounts[0] = new Account("Aarav Sharma", 5000);
        accounts[1] = new Account("Priya Patel", 10000);
        accounts[2] = new Account("Rohan Mehta");

        System.out.println("\n--- Initial Account Details ---");
        for (Account acc : accounts) {
            System.out.println("Account: " + acc.getAccountNumber() + " | Owner: " + acc.getOwnerName() + " | Balance: " + acc.getBalance());
        }

        accounts[0].deposit(2500);
        accounts[1].withdraw(3000);
        boolean withdrawStatus = accounts[2].withdraw(1000);

        System.out.println("\n--- Transaction Executed ---");
        System.out.println("Attempted withdrawal of 1000 from " + accounts[2].getAccountNumber() + ": " + (withdrawStatus ? "Successful" : "Failed (Insufficient Balance)"));

        System.out.println("\n--- Updated Account Balances ---");
        for (Account acc : accounts) {
            System.out.println("Account: " + acc.getAccountNumber() + " | Owner: " + acc.getOwnerName() + " | Balance: " + acc.getBalance());
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
}