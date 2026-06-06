package ro.app.account.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;

/**
 * Teste unitare pentru entitatea Account.
 *
 * Testam constructorii si valorile implicite (default).
 * Nu avem nevoie de Mockito — Account este un POJO / JPA Entity.
 *
 * Pattern testat: constructorul cu parametri (partial Creational).
 */
@RunWith(JUnit4.class)
public class AccountTest {

    @Test
    public void defaultConstructor_balanceDefaultsToNull() {
        // Arrange & Act
        Account account = new Account();

        // Assert — constructorul implicit nu seteaza valori
        Assert.assertNull(account.getId());
        Assert.assertNull(account.getIban());
        Assert.assertNull(account.getClientId());
    }

    @Test
    public void twoParamConstructor_setsIbanCurrencyClient() {
        // Arrange & Act
        Account account = new Account("RO49BANK0000000000000001", CurrencyType.EUR, 123L);

        // Assert
        Assert.assertEquals("RO49BANK0000000000000001", account.getIban());
        Assert.assertEquals(CurrencyType.EUR, account.getCurrency());
        Assert.assertEquals(Long.valueOf(123L), account.getClientId());
        Assert.assertEquals(AccountStatus.ACTIVE, account.getStatus());
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    public void fullConstructor_setsAllFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 15, 10, 30);
        LocalDateTime updated = LocalDateTime.of(2024, 6, 1, 12, 0);

        // Act
        Account account = new Account(
                "RO49BANK0000000000000002",
                BigDecimal.valueOf(1500.00),
                CurrencyType.USD,
                456L,
                AccountStatus.SUSPENDED,
                created,
                updated);

        // Assert
        Assert.assertEquals("RO49BANK0000000000000002", account.getIban());
        Assert.assertEquals(0, BigDecimal.valueOf(1500.00).compareTo(account.getBalance()));
        Assert.assertEquals(CurrencyType.USD, account.getCurrency());
        Assert.assertEquals(Long.valueOf(456L), account.getClientId());
        Assert.assertEquals(AccountStatus.SUSPENDED, account.getStatus());
        Assert.assertEquals(created, account.getCreatedAt());
        Assert.assertEquals(updated, account.getUpdatedAt());
    }

    @Test
    public void fullConstructor_nullBalance_defaultsToZero() {
        // Arrange & Act — Boundary: null balance => BigDecimal.ZERO
        Account account = new Account("RO49BANK0000000000000003", null,
                CurrencyType.RON, 789L, AccountStatus.ACTIVE, LocalDateTime.now(), null);

        // Assert
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    public void fullConstructor_nullStatus_defaultsToActive() {
        // Arrange & Act — null status => ACTIVE
        Account account = new Account("RO49BANK0000000000000004", BigDecimal.ZERO,
                CurrencyType.GBP, 111L, null, LocalDateTime.now(), null);

        // Assert
        Assert.assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    public void setters_updateFieldsCorrectly() {
        // Arrange
        Account account = new Account();

        // Act
        account.setId(10L);
        account.setIban("RO49BANK0000000000000005");
        account.setBalance(BigDecimal.valueOf(200));
        account.setCurrency(CurrencyType.EUR);
        account.setClientId(77L);
        account.setStatus(AccountStatus.CLOSED);

        // Assert
        Assert.assertEquals(Long.valueOf(10L), account.getId());
        Assert.assertEquals("RO49BANK0000000000000005", account.getIban());
        Assert.assertEquals(0, BigDecimal.valueOf(200).compareTo(account.getBalance()));
        Assert.assertEquals(CurrencyType.EUR, account.getCurrency());
        Assert.assertEquals(Long.valueOf(77L), account.getClientId());
        Assert.assertEquals(AccountStatus.CLOSED, account.getStatus());
    }
}
