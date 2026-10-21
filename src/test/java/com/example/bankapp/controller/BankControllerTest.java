
package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private BankController controller;

    private Account account;
    private Model model;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setUsername("alice");
        account.setBalance(new BigDecimal("100"));

        model = new ExtendedModelMap();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice", "password"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardReturnsAccount() {
        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);

        assertEquals("dashboard", controller.dashboard(model));
        assertSame(account, model.getAttribute("account"));
    }

    @Test
    void showRegistrationFormReturnsRegister() {
        assertEquals("register", controller.showRegistrationForm());
    }

    @Test
    void registerAccountRedirectsOnSuccess() {
        when(accountService.registerAccount("alice", "password"))
                .thenReturn(account);

        assertEquals(
                "redirect:/login",
                controller.registerAccount("alice", "password", model));
    }

    @Test
    void registerAccountDisplaysErrorOnFailure() {
        when(accountService.registerAccount("alice", "password"))
                .thenThrow(new RuntimeException("Username already exists"));

        assertEquals(
                "register",
                controller.registerAccount("alice", "password", model));

        assertEquals(
                "Username already exists",
                model.getAttribute("error"));
    }

    @Test
    void loginReturnsLogin() {
        assertEquals("login", controller.login());
    }

    @Test
    void depositRedirectsToDashboard() {
        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);

        assertEquals(
                "redirect:/dashboard",
                controller.deposit(new BigDecimal("20")));

        verify(accountService).deposit(account, new BigDecimal("20"));
    }

    @Test
    void withdrawRedirectsOnSuccess() {
        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);

        assertEquals(
                "redirect:/dashboard",
                controller.withdraw(new BigDecimal("20"), model));

        verify(accountService).withdraw(account, new BigDecimal("20"));
    }

    @Test
    void withdrawDisplaysErrorOnFailure() {
        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);

        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService)
                .withdraw(account, new BigDecimal("200"));

        assertEquals(
                "dashboard",
                controller.withdraw(new BigDecimal("200"), model));

        assertEquals("Insufficient funds", model.getAttribute("error"));
        assertSame(account, model.getAttribute("account"));
    }

    @Test
    void transactionHistoryReturnsTransactions() {
        List<Transaction> transactions =
                List.of(mock(Transaction.class));

        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);
        when(accountService.getTransactionHistory(account))
                .thenReturn(transactions);

        assertEquals("transactions", controller.transactionHistory(model));
        assertSame(transactions, model.getAttribute("transactions"));
    }

    @Test
    void transferRedirectsOnSuccess() {
        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);

        assertEquals(
                "redirect:/dashboard",
                controller.transferAmount(
                        "bob", new BigDecimal("20"), model));

        verify(accountService).transferAmount(
                account, "bob", new BigDecimal("20"));
    }

    @Test
    void transferDisplaysErrorOnFailure() {
        when(accountService.findAccountByUsername("alice"))
                .thenReturn(account);

        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService)
                .transferAmount(account, "bob", new BigDecimal("20"));

        assertEquals(
                "dashboard",
                controller.transferAmount(
                        "bob", new BigDecimal("20"), model));

        assertEquals(
                "Recipient account not found",
                model.getAttribute("error"));
        assertSame(account, model.getAttribute("account"));
    }
}
