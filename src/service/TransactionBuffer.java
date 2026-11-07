package service;

public class TransactionBuffer {
    private final Runnable[] transactions;
    private int head;
    private int tail;
    private int size;
    private boolean closed;

    public TransactionBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive.");
        }
        transactions = new Runnable[capacity];
    }

    public synchronized void add(Runnable transaction) throws InterruptedException {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }
        while (size == transactions.length && !closed) {
            wait();
        }
        if (closed) {
            throw new IllegalStateException("Cannot add a transaction to a closed buffer.");
        }

        transactions[tail] = transaction;
        tail = (tail + 1) % transactions.length;
        size++;
        notifyAll();
    }

    public synchronized Runnable take() throws InterruptedException {
        while (size == 0 && !closed) {
            wait();
        }
        if (size == 0) {
            return null;
        }

        Runnable transaction = transactions[head];
        transactions[head] = null;
        head = (head + 1) % transactions.length;
        size--;
        notifyAll();
        return transaction;
    }

    public synchronized void close() {
        closed = true;
        notifyAll();
    }
}
