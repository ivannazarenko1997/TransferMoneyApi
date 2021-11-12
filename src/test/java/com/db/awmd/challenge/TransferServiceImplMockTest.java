package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.AccountNotProcessedExeption;
import com.db.awmd.challenge.exception.OverDraftException;
import com.db.awmd.challenge.exception.SameOperationalAccountException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.TransferService;
import com.db.awmd.challenge.service.TransferServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;


@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferServiceImplMockTest {

    private static final String ACCOUNT_FROM = "acc-1";
    private static final String ACCOUNT_TO = "acc-2";
    private static final BigDecimal AMOUNT_5 = new BigDecimal(5);
    private static final BigDecimal AMOUNT_10 = new BigDecimal(10);
    private static final BigDecimal AMOUNT_BELOW_ZERO = new BigDecimal(-10);
    private static final BigDecimal INITIAL_BALANCE_0 = new BigDecimal(0);
    private static final BigDecimal INITIAL_BALANCE_10 = new BigDecimal(10);
    private static final Account ACCOUNT_1_BALANCE_0 = new Account(ACCOUNT_FROM, INITIAL_BALANCE_0);
    private static final Account ACCOUNT_2_BALANCE_0 = new Account(ACCOUNT_TO, INITIAL_BALANCE_0);
    private static final Account ACCOUNT_1_BALANCE_5 = new Account(ACCOUNT_FROM, AMOUNT_5);
    private static final Account ACCOUNT_1_BALANCE_10 = new Account(ACCOUNT_FROM, AMOUNT_10);


    @Mock
    private AccountsService accountsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService = new TransferServiceImpl(accountsService,  notificationService);

    @Before
    public void before() {
        Account account1 = new Account(ACCOUNT_FROM);
        accountsService.createAccount(account1);
        transferService = new TransferServiceImpl(accountsService,   notificationService);
    }

    @Test(expected = AccountNotExistException.class)
    public void shouldReturnAccountNotFoundExceptionIfAccount_1_NotExists() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(null);
        Transfer transfer = new Transfer(ACCOUNT_FROM, ACCOUNT_TO, AMOUNT_10);
        transferService.transferMoneyLock(transfer);
        Mockito.verify(accountsService, never()).makeTransfer(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(notificationService, never()).notifyAboutTransfer(Mockito.any() , Mockito.anyString());
        Mockito.verify(accountsService.getAccount(ACCOUNT_FROM));
        assertEquals(accountsService.findAccountById(ACCOUNT_FROM).getBalance(), INITIAL_BALANCE_0);
        assertEquals(accountsService.findAccountById(ACCOUNT_TO).getBalance(), INITIAL_BALANCE_0);
    }

    @Test(expected = AccountNotExistException.class)
    public void shouldReturnAccountNotFoundExceptionIfAccount_2_NotExists() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_0 );
        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(null);
        Transfer transfer = new Transfer(ACCOUNT_FROM, ACCOUNT_TO, AMOUNT_10);
        transferService.transferMoneyLock(transfer);
        Mockito.verify(accountsService.findAccountById(ACCOUNT_FROM));
        Mockito.verify(accountsService.findAccountById(ACCOUNT_TO));
        Mockito.verify(accountsService, never()).makeTransfer(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(notificationService, never()).notifyAboutTransfer(Mockito.any() , Mockito.anyString());
        assertEquals(accountsService.findAccountById(ACCOUNT_FROM).getBalance(), INITIAL_BALANCE_0);
        assertEquals(accountsService.findAccountById(ACCOUNT_TO).getBalance(), INITIAL_BALANCE_0);
    }

    @Test(expected = AccountNotProcessedExeption.class)
    public void shouldReturnAccountNotProcessedExeptionIfAmountIsBelowZero() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_0 );
        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(ACCOUNT_2_BALANCE_0 );
        Transfer transfer = new Transfer(ACCOUNT_FROM, ACCOUNT_TO, AMOUNT_BELOW_ZERO);
        transferService.transferMoneyLock(transfer);
        Mockito.verify(accountsService, never()).makeTransfer(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(notificationService, never()).notifyAboutTransfer(Mockito.any() , Mockito.anyString());

        Mockito.verify(accountsService.findAccountById(ACCOUNT_FROM));
        Mockito.verify(accountsService.findAccountById(ACCOUNT_TO));
        assertEquals(accountsService.findAccountById(ACCOUNT_FROM).getBalance(), INITIAL_BALANCE_0);
        assertEquals(accountsService.findAccountById(ACCOUNT_TO).getBalance(), INITIAL_BALANCE_0);
    }

    @Test(expected = SameOperationalAccountException.class)
    public void shouldReturnSameOperationalAccountException() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_0 );
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_0 );
        Transfer transfer = new Transfer(ACCOUNT_FROM, ACCOUNT_FROM, AMOUNT_10);
        transferService.transferMoneyLock(transfer);
        Mockito.verify(accountsService, never()).makeTransfer(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(notificationService, never()).notifyAboutTransfer(Mockito.any() , Mockito.anyString());

        Mockito.verify(accountsService.findAccountById(ACCOUNT_FROM));
        Mockito.verify(accountsService.findAccountById(ACCOUNT_FROM));
        assertEquals(accountsService.findAccountById(ACCOUNT_FROM).getBalance(), INITIAL_BALANCE_0);
    }

    @Test(expected = OverDraftException.class)
    public void shouldReturnOverDraftExceptionThenNotEnoughtBalance() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_5);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(ACCOUNT_2_BALANCE_0 );
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_5);

        Transfer transfer = new Transfer(ACCOUNT_FROM, ACCOUNT_TO, AMOUNT_10);
        transferService.transferMoneyLock(transfer);
        Mockito.verify(accountsService.findAccountById(ACCOUNT_FROM));
        Mockito.verify(accountsService.findAccountById(ACCOUNT_TO));
        Mockito.verify(accountsService, never()).makeTransfer(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(notificationService, never()).notifyAboutTransfer(Mockito.any() , Mockito.anyString());
        Mockito.verify(accountsService.findAccountById(ACCOUNT_FROM));
        assertEquals(accountsService.findAccountById(ACCOUNT_FROM).getBalance(), ACCOUNT_1_BALANCE_5);
        assertEquals(accountsService.findAccountById(ACCOUNT_FROM).getBalance(), INITIAL_BALANCE_0);
    }

    @Test
    public void shouldProcessAllAndSendMessages() {
        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_10);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(ACCOUNT_2_BALANCE_0 );

        Mockito.when(accountsService.findAccountById(ACCOUNT_FROM)).thenReturn(ACCOUNT_1_BALANCE_10);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(ACCOUNT_2_BALANCE_0 );

        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(ACCOUNT_2_BALANCE_0 );
        Mockito.when(accountsService.findAccountById(ACCOUNT_TO)).thenReturn(ACCOUNT_2_BALANCE_0 );


        Transfer transfer = new Transfer(ACCOUNT_FROM, ACCOUNT_TO, AMOUNT_10);
        transferService.transferMoneyLock(transfer);

        Mockito.verify(accountsService).makeTransfer(ACCOUNT_1_BALANCE_10, ACCOUNT_2_BALANCE_0,AMOUNT_10);
        InOrder orderVerifier = Mockito.inOrder(accountsService,notificationService);

        assertTrue(ACCOUNT_FROM.hashCode()<ACCOUNT_TO.hashCode());
        orderVerifier.verify(accountsService).makeTransfer(ACCOUNT_1_BALANCE_10, ACCOUNT_2_BALANCE_0,AMOUNT_10);

        String debitMessageUser = "Monney was send from your account to acc-2 in amount 10";
        Account newDebitAccount = new Account(ACCOUNT_FROM, INITIAL_BALANCE_10);
        orderVerifier.verify(notificationService).notifyAboutTransfer(newDebitAccount, debitMessageUser);
        String creditMessageUser = "Your account was deposit from acc-1 in amount 10";
        Account newCreditAccount = new Account(ACCOUNT_TO, INITIAL_BALANCE_0);

        orderVerifier.verify(notificationService).notifyAboutTransfer(newCreditAccount, creditMessageUser);
   }


}
