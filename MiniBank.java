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

        System.out.println("\n--- Testing Validators ---");
        System.out.println("Mobile (9876543210): " + Validator.isValidMobile("9876543210"));
        System.out.println("Mobile (12345): " + Validator.isValidMobile("12345"));

        System.out.println("Email (user@domain.com): " + Validator.isValidEmail("user@domain.com"));
        System.out.println("Email (userdomain.com): " + Validator.isValidEmail("userdomain.com"));

        System.out.println("PAN (ABCDE1234F): " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("PAN (ABC1234F): " + Validator.isValidPan("ABC1234F"));

        System.out.println("IFSC (SBIN0001234): " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("IFSC (SBIN1001234): " + Validator.isValidIfsc("SBIN1001234"));

        System.out.println("\n--- Testing CommandParser ---");
        String sampleCommand = "DEPOSIT AC0001 500";
        Command command = CommandParser.parse(sampleCommand);
        System.out.println("Parsed Command Type: " + command.type());
        System.out.println("Parsed Account Number: " + command.accountNumber());
        System.out.println("Parsed Amount: " + command.amount());

        System.out.println("\n--- Testing StatementFormatter ---");
        Account testAccount = new Account("Aarav Sharma", 15000);
        System.out.println(StatementFormatter.buildStatement(testAccount));

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