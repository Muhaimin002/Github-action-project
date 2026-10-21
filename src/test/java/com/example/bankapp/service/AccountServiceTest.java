
package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account createAccount(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("encoded-password");
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void findAccountByUsernameReturnsAccount() {
        Account account = createAccount("alice", "100");

        when(accountRepository.findByUsername("alice"))
                .thenReturn(Optional.of(account));

        assertSame(account, accountService.findAccountByUsername("alice"));
    }

    @Test
    void findAccountByUsernameThrowsWhenMissing() {
        when(accountRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.findAccountByUsername("missing"));

        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void registerAccountCreatesNewAccount() {
        when(accountRepository.findByUsername("alice"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("alice", "password123");

        assertEquals("alice", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccountRejectsDuplicateUsername() {
        when(accountRepository.findByUsername("alice"))
                .thenReturn(Optional.of(createAccount("alice", "100")));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.registerAccount("alice", "password123"));

        assertEquals("Username already exists", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void depositUpdatesBalanceAndSavesTransaction() {
        Account account = createAccount("alice", "100");

        accountService.deposit(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdrawUpdatesBalanceAndSavesTransaction() {
        Account account = createAccount("alice", "100");

        accountService.withdraw(account, new BigDecimal("30"));

        assertEquals(new BigDecimal("70"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        Account account = createAccount("alice", "20");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50")));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistoryReturnsTransactions() {
        Account account = createAccount("alice", "100");
        List<Transaction> transactions = List.of(mock(Transaction.class));

        when(transactionRepository.findByAccountId(account.getId()))
                .thenReturn(transactions);

        assertSame(transactions, accountService.getTransactionHistory(account));
    }

    @Test
    void loadUserByUsernameReturnsUserDetails() {
        Account account = createAccount("alice", "100");

        when(accountRepository.findByUsername("alice"))
                .thenReturn(Optional.of(account));

        UserDetails user = accountService.loadUserByUsername("alice");

        assertEquals("alice", user.getUsername());
        assertEquals("encoded-password", user.getPassword());
        assertTrue(user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("USER")));
    }

    @Test
    void authoritiesReturnsUserAuthority() {
        assertTrue(accountService.authorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("USER")));
    }

    @Test
    void transferAmountUpdatesBothBalances() {
        Account sender = createAccount("alice", "100");
        Account recipient = createAccount("bob", "40");

        when(accountRepository.findByUsername("bob"))
                .thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("30"));

        assertEquals(new BigDecimal("70"), sender.getBalance());
        assertEquals(new BigDecimal("70"), recipient.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmountRejectsInsufficientFunds() {
        Account sender = createAccount("alice", "10");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.transferAmount(
                        sender, "bob", new BigDecimal("20")));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferAmountRejectsMissingRecipient() {
        Account sender = createAccount("alice", "100");

        when(accountRepository.findByUsername("bob"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.transferAmount(
                        sender, "bob", new BigDecimal("20")));

        assertEquals("Recipient account not found", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }
}


