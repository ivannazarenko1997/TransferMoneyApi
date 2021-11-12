package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.OverDraftException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.AccountsServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceMockTest {
    private static final String ACCOUNT_FROM = "acc-1";
    private static final String ACCOUNT_TO = "acc-2";

    private static final BigDecimal INITIAL_BALANCE_0 = new BigDecimal(0);
    private static final BigDecimal INITIAL_BALANCE_10 = new BigDecimal(10);
    private static final BigDecimal TRANSFER_VALUE_10 = new BigDecimal(10);


    @Mock
    private AccountsRepository accountsRepository;

    @InjectMocks
    private AccountsService accountsService = new AccountsServiceImpl(accountsRepository );


    @Before
    public void before() {
       accountsService = new AccountsServiceImpl(accountsRepository );
    }


    @Test(expected = OverDraftException.class)
    public void shouldReturnOverDraftExceptionAndNotUpdateBalanceThenWithdraw() throws Exception {
        this.accountsService.clearAccounts();
        Account accountFrom = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
        this.accountsService.createAccount(accountFrom);

        Mockito.when(accountsRepository.getAccount(ACCOUNT_FROM)).thenReturn(accountFrom );
        accountsService.debitBalanceAccount(ACCOUNT_FROM,TRANSFER_VALUE_10);
        Mockito.verify(accountsService).findAccountById(ACCOUNT_FROM);
        assertThat(accountsService.getAccount(ACCOUNT_FROM)).isEqualTo(new Account(ACCOUNT_FROM, INITIAL_BALANCE_0));
        Mockito.verify(accountsRepository, never()).updateAccount(accountFrom);

    }

    @Test(expected = AccountNotExistException.class)
    public void shouldReturnAccountNotExistExceptionAndNotUpdateBalanceThenWithdraw() throws Exception {
        this.accountsService.clearAccounts();

        Mockito.when(accountsRepository.getAccount(ACCOUNT_FROM)).thenReturn(null);
        accountsService.debitBalanceAccount(ACCOUNT_FROM,TRANSFER_VALUE_10);

        assertThat(accountsService.getAccount(ACCOUNT_FROM)).isEqualTo(null);
        Mockito.verify(accountsRepository, never()).updateAccount(Mockito.any());

    }


    @Test(expected = AccountNotExistException.class)
    public void shouldReturnAccountNotExistExceptionAndNotUpdateBalanceThenDeposit() throws Exception {
        this.accountsService.clearAccounts();

        Mockito.when(accountsRepository.getAccount(ACCOUNT_TO)).thenReturn(null);
        accountsService.creditBalanceAccount(ACCOUNT_TO,TRANSFER_VALUE_10);

        assertThat(accountsService.getAccount(ACCOUNT_TO)).isEqualTo(null);
        Mockito.verify(accountsRepository, never()).updateAccount(Mockito.any());

    }


    @Test(expected = AccountNotExistException.class)
    public void shouldReturnAccountNotExistExceptionAccountNotExists() throws Exception {
        this.accountsService.clearAccounts();

        Mockito.when(accountsRepository.getAccount(ACCOUNT_TO)).thenReturn(null);
        accountsService.findAccountById(ACCOUNT_TO);

        Mockito.verify(accountsRepository).getAccount(ACCOUNT_TO);
        assertThat(accountsService.getAccount(ACCOUNT_TO)).isEqualTo(null);
    }

    @Test
    public void shouldReturnAccountThenFindAccountById() throws Exception {
        this.accountsService.clearAccounts();
        Account accountTo = new Account(ACCOUNT_TO, INITIAL_BALANCE_0);
        this.accountsService.createAccount(accountTo);

        Mockito.when(accountsRepository.getAccount(ACCOUNT_TO)).thenReturn(accountTo);
        accountsService.findAccountById(ACCOUNT_TO);

        Mockito.verify(accountsRepository).getAccount(ACCOUNT_TO);
        assertThat(accountsService.getAccount(ACCOUNT_TO)).isEqualTo(new Account(ACCOUNT_TO, INITIAL_BALANCE_0));
    }

    @Test
    public void shouldUpdateBalanceOfAccountThenDeposit() throws Exception {
        this.accountsService.clearAccounts();
        Account accountTo = new Account(ACCOUNT_TO, INITIAL_BALANCE_0);
        this.accountsService.createAccount(accountTo);


        Mockito.when(accountsRepository.getAccount(ACCOUNT_TO)).thenReturn(accountTo );

        accountsService.creditBalanceAccount(ACCOUNT_TO,TRANSFER_VALUE_10);

        Mockito.verify(accountsRepository).updateAccount(accountTo);
        assertThat(accountsService.getAccount(ACCOUNT_TO)).isEqualTo(new Account(ACCOUNT_TO, INITIAL_BALANCE_10));
    }

    @Test
    public void shouldUpdateBalanceOfAccountThenWithdraw() throws Exception {
        this.accountsService.clearAccounts();
        Account accountFrom = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        this.accountsService.createAccount(accountFrom);


        Mockito.when(accountsRepository.getAccount(ACCOUNT_FROM)).thenReturn(accountFrom );
        accountsService.debitBalanceAccount(ACCOUNT_FROM,TRANSFER_VALUE_10);

        assertThat(accountsService.getAccount(ACCOUNT_FROM)).isEqualTo(new Account(ACCOUNT_FROM, INITIAL_BALANCE_0));
        Mockito.verify(accountsRepository).updateAccount(accountFrom);
    }

}
