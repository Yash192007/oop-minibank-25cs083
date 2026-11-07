package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import exception.InsufficientFundsException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.Repository;

class BankServiceTest {
    @TempDir
    Path tempDirectory;

    private BankService bankService;

    @BeforeEach
    void setUp() throws Exception {
        bankService = new BankService(
                new Repository<>(),
                new TransactionProcessor(),
                tempDirectory.resolve("accounts.ser"),
                tempDirectory.resolve("logs/transactions.log"));
    }

    @AfterEach
    void tearDown() throws IOException {
        bankService.close();
    }

    @Test
    void depositIncreasesBalance() throws Exception {
        var account = bankService.openAccount(
                "Test Customer", "test@example.com", "9876543210", "SAVINGS", 500);

        bankService.deposit(account.getAccountNumber(), 200);

        assertEquals(700, account.getBalance());
    }

    @Test
    void withdrawalOverLimitThrowsInsufficientFunds() throws Exception {
        var account = bankService.openAccount(
                "Test Customer", "test@example.com", "9876543210", "SAVINGS", 500);

        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> bankService.withdraw(account.getAccountNumber(), 600));

        assertEquals(100, exception.getShortfall());
        assertEquals(500, account.getBalance());
    }

    @Test
    void concurrentBatchDepositsKeepBalanceCorrect() throws Exception {
        var account = bankService.openAccount(
                "Test Customer", "test@example.com", "9876543210", "SAVINGS", 100);
        ArrayList<BankService.TransactionRequest> transactions = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            transactions.add(new BankService.TransactionRequest(
                    BankService.TransactionType.DEPOSIT,
                    account.getAccountNumber(),
                    1));
        }

        bankService.processBatch(transactions);

        assertEquals(1_100, account.getBalance());
    }
}
