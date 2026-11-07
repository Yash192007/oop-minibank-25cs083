package util;

public class CommandParser {
    public static Command parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Input command line cannot be empty.");
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Command must contain action, account number, and amount.");
        }

        TransactionType type = TransactionType.valueOf(parts[0].toUpperCase());
        String accountNumber = parts[1];
        long amount = Long.parseLong(parts[2]);

        return new Command(type, accountNumber, amount);
    }
}