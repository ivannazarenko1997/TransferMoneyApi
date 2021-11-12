package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.OverDraftException;
import com.db.awmd.challenge.service.AccountsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {
    private static final String ACCOUNT_FROM = "acc-1";
    private static final String ACCOUNT_TO = "acc-2";

    private static final BigDecimal INITIAL_BALANCE_0 = new BigDecimal(0);
    private static final BigDecimal INITIAL_BALANCE_10 = new BigDecimal(10);
    private static final BigDecimal TRANSFER_VALUE_10 = new BigDecimal(10);


    @Autowired
    private AccountsService accountsService;

    @Test
    public void addAccount() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);
        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    public void addAccount_failsOnDuplicateId() throws Exception {
        this.accountsService.clearAccounts();
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }

    }

    @Test
    public void shouldReturnAccount() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        this.accountsService.createAccount(account);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        this.accountsService.clearAccounts();
    }

    @Test(expected = AccountNotExistException.class)
    public void shouldReturnErrorAccountNotExists() throws Exception {
        this.accountsService.clearAccounts();
        accountsService.findAccountById(ACCOUNT_FROM);
    }

    @Test
    public void shouldUpdateAccount() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        this.accountsService.createAccount(account);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        Account updatedAccount = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        accountsService.updateAccount(updatedAccount);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(updatedAccount);
        this.accountsService.clearAccounts();
    }

    @Test
    public void shouldAddBalanceToAccount() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        this.accountsService.createAccount(account);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        this.accountsService.creditBalanceAccount(ACCOUNT_FROM, TRANSFER_VALUE_10);
        Account accountCredited = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(accountCredited);
        this.accountsService.clearAccounts();
    }

    @Test
    public void shouldWithdrawBalanceFromAccount() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        this.accountsService.createAccount(account);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        this.accountsService.debitBalanceAccount(ACCOUNT_FROM, TRANSFER_VALUE_10);
        Account accountDebited = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(accountDebited);
        this.accountsService.clearAccounts();
    }

    @Test(expected = OverDraftException.class)
    public void shouldReturnOverDraftExceptionThenWithdrawAmountWithZeroBalance() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        this.accountsService.createAccount(account);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        this.accountsService.debitBalanceAccount(ACCOUNT_FROM, TRANSFER_VALUE_10);
        Account accountDebited = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(accountDebited);
        this.accountsService.clearAccounts();
    }

    @Test
    public void shouldProcessTransfer() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        this.accountsService.createAccount(account);

        Account account2 = new Account(ACCOUNT_TO, INITIAL_BALANCE_0);
        this.accountsService.createAccount(account2);

        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        assertThat(accountsService.findAccountById(ACCOUNT_TO)).isEqualTo(account2);

        this.accountsService.makeTransfer(ACCOUNT_FROM, ACCOUNT_TO, TRANSFER_VALUE_10);

        Account accountDebited = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(accountDebited);

        Account accountCredited = new Account(ACCOUNT_TO, INITIAL_BALANCE_10);
        assertThat(accountsService.findAccountById(ACCOUNT_TO)).isEqualTo(accountCredited);
        this.accountsService.clearAccounts();
    }

    @Test(expected = OverDraftException.class)
    public void shouldLeftBalancesBeforeThenReturnOverdraftException() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        this.accountsService.createAccount(account);

        Account account2 = new Account(ACCOUNT_TO, INITIAL_BALANCE_10);
        this.accountsService.createAccount(account2);

        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        assertThat(accountsService.findAccountById(ACCOUNT_TO)).isEqualTo(account2);

        this.accountsService.makeTransfer(ACCOUNT_FROM, ACCOUNT_TO, TRANSFER_VALUE_10);

        Account accountDebited = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(accountDebited);

        Account accountCredited = new Account(ACCOUNT_TO, INITIAL_BALANCE_10);
        assertThat(accountsService.findAccountById(ACCOUNT_TO)).isEqualTo(accountCredited);
        this.accountsService.clearAccounts();
    }

    @Test(expected = AccountNotExistException.class)
    public void shouldRollbackDebitOperationThenCreditAccountNotExist() throws Exception {
        this.accountsService.clearAccounts();
        Account account = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        this.accountsService.createAccount(account);

        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(account);
        assertThat(accountsService.findAccountById(ACCOUNT_TO)).isEqualTo(null);

        this.accountsService.makeTransfer(ACCOUNT_FROM, ACCOUNT_TO, TRANSFER_VALUE_10);

        Account accountDebited = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        assertThat(accountsService.findAccountById(ACCOUNT_FROM)).isEqualTo(accountDebited);
        assertThat(accountsService.findAccountById(ACCOUNT_TO)).isEqualTo(null);

        this.accountsService.clearAccounts();
    }

}
