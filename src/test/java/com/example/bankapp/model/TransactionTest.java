
package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void noArgumentConstructorAndSettersWork() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        LocalDateTime timestamp = LocalDateTime.now();

        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("25.50"));
        transaction.setType("Deposit");
        transaction.setTimestamp(timestamp);
        transaction.setAccount(account);

        assertEquals(1L, transaction.getId());
        assertEquals(new BigDecimal("25.50"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertSame(account, transaction.getAccount());
    }

    @Test
    void parameterizedConstructorSetsAllFields() {
        Account account = new Account();
        LocalDateTime timestamp = LocalDateTime.now();

        Transaction transaction = new Transaction(
                new BigDecimal("40"), "Withdrawal", timestamp, account);

        assertEquals(new BigDecimal("40"), transaction.getAmount());
        assertEquals("Withdrawal", transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertSame(account, transaction.getAccount());
    }
}
