package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

public class ReportGenerator {
    public static String generate(Path logsDirectory, Path reportFile) throws IOException {
        long totalDeposits = 0;
        long totalWithdrawals = 0;
        StringBuilder fileDetails = new StringBuilder();

        Files.createDirectories(logsDirectory);
        try (DirectoryStream<Path> logFiles = Files.newDirectoryStream(logsDirectory, "*.log")) {
            for (Path logFile : logFiles) {
                if (!Files.isRegularFile(logFile)) {
                    continue;
                }

                BasicFileAttributes attributes = Files.readAttributes(logFile, BasicFileAttributes.class);
                fileDetails.append(logFile.getFileName())
                        .append(" | size=").append(attributes.size())
                        .append(" bytes | last-modified=")
                        .append(Instant.ofEpochMilli(attributes.lastModifiedTime().toMillis()))
                        .append(System.lineSeparator());

                try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",", 2);
                        if (parts.length != 2) {
                            throw new IOException("Malformed transaction log line in " + logFile + ": " + line);
                        }

                        long amount;
                        try {
                            amount = Long.parseLong(parts[1]);
                        } catch (NumberFormatException e) {
                            throw new IOException("Invalid transaction amount in " + logFile + ": " + line, e);
                        }
                        if (amount < 0) {
                            throw new IOException("Transaction amount cannot be negative in " + logFile + ": " + line);
                        }

                        switch (parts[0]) {
                            case "DEPOSIT" -> totalDeposits = Math.addExact(totalDeposits, amount);
                            case "WITHDRAWAL" -> totalWithdrawals = Math.addExact(totalWithdrawals, amount);
                            default -> throw new IOException(
                                    "Unknown transaction type in " + logFile + ": " + parts[0]);
                        }
                    }
                }
            }
        } catch (ArithmeticException e) {
            throw new IOException("Transaction totals exceed the supported long range.", e);
        }

        String report = "End-of-day transaction summary" + System.lineSeparator()
                + "Total deposits: " + totalDeposits + System.lineSeparator()
                + "Total withdrawals: " + totalWithdrawals + System.lineSeparator()
                + "Log file details:" + System.lineSeparator()
                + fileDetails;
        Path parent = reportFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                reportFile,
                report,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        return report;
    }
}
