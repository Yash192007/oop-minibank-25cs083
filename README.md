# MiniBank

MiniBank is a Java command-line banking application demonstrating object-oriented
design, runtime annotations, checked exceptions, concurrent transactions,
collections, generic repositories, serialization, and NIO reporting.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer

## Build, test, and run

From the project root:

```powershell
mvn test
mvn package
java -jar target/oop-minibank-1.0.0.jar
```

The runnable JAR declares `service.MiniBank` as its main class. JUnit 5 tests
cover deposits, insufficient-funds withdrawals, and concurrent deposits.

## Menu

1. Open a savings, current, or fixed-deposit account
2. Deposit
3. Withdraw
4. Transfer
5. Print an account statement
6. List accounts
7. Process a deposit/withdrawal batch on the managed thread pool
8. Generate an end-of-day report
9. Save and exit

Account creation validates email and mobile formats. Withdrawals and transfers
report explicit bank exceptions, including withdrawal shortfalls.

## Runtime files

The application reads and writes paths relative to its working directory:

- `data/accounts.ser` — serialized account state, loaded at startup and saved
  when the application exits.
- `logs/transactions.log` — append-only deposit and withdrawal entries.
- `reports/end-of-day.txt` — generated totals and log-file metadata.

Back up serialized state and logs before editing or removing them.

## Source layout

- `model` — accounts, customers, and account capabilities
- `model.annotation` — runtime field annotations
- `repository` — generic reflection-based repository
- `service` — banking operations, menu, and transaction processing
- `util` — validation, persistence, transaction logs, and reports
- `exception` — checked banking exception hierarchy
