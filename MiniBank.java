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

        Customer.Address address = new Customer.Address("123 Station Road", "Ahmedabad", "380001");
        Customer customer = new Customer("Aarav Sharma", "aarav@example.com", "9876543210", address);

        Account acc1 = new Account("Aarav Sharma", 5000);
        Account acc2 = new Account("Priya Patel", 10000);
        Account acc3 = new Account(acc1.getAccountNumber(), "Aarav Duplicate", 5000);

        System.out.println("\n--- Testing toString() ---");
        System.out.println(acc1);
        System.out.println(acc2);

        System.out.println("\n--- Testing equals() and hashCode() ---");
        System.out.println("acc1 equals acc2: " + acc1.equals(acc2));
        System.out.println("acc1 equals acc3 (Same Account Number): " + acc1.equals(acc3));

        System.out.println("\n--- Testing instanceof Operator ---");
        Object obj1 = acc1;
        Object obj2 = customer;

        if (obj1 instanceof Account) {
            System.out.println("obj1 is an instance of Account: " + ((Account) obj1).getAccountNumber());
        }

        if (obj2 instanceof Customer) {
            System.out.println("obj2 is an instance of Customer: " + ((Customer) obj2).getName() + " from " + ((Customer) obj2).getAddress());
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