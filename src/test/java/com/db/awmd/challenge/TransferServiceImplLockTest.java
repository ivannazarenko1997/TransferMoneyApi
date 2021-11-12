package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;


@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferServiceImplLockTest {
    private static final Integer FIRST_LOCK = 0;
    private static final Integer SECOND_LOCK =1;
    private static final String ACCOUNT_ONE = "acc-1";
    private static final String ACCOUNT_TWO = "acc-2";
    private static final String ACCOUNT_THREE = "acc-3";
    private static final String ACCOUNT_FOUR = "acc-4";
    private static final BigDecimal AMOUNT_10 = new BigDecimal(10);
    private static final BigDecimal INITIAL_BALANCE_0 = new BigDecimal(0);
    private static final Account ACCOUNT_1_BALANCE_0 = new Account(ACCOUNT_ONE, INITIAL_BALANCE_0);
    private static final Account ACCOUNT_2_BALANCE_0 = new Account(ACCOUNT_TWO, INITIAL_BALANCE_0);
    private static final Account ACCOUNT_1_BALANCE_10 = new Account(ACCOUNT_ONE, AMOUNT_10);
    private static final Account ACCOUNT_2_BALANCE_10 = new Account(ACCOUNT_TWO, AMOUNT_10);


    @Mock
    private AccountsService accountsService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferServiceImpl transferService = new TransferServiceImpl(accountsService,  notificationService);

    @Before
    public void before() {
        Account account1 = new Account(ACCOUNT_ONE,AMOUNT_10);
        accountsService.createAccount(account1);
        transferService = new TransferServiceImpl(accountsService,   notificationService);
    }

    @Test
    public void checkSortIdGenerationAlgoritm() {
        Account accountFirst = new Account(ACCOUNT_ONE);
        Long expectedSortedFirstId = Long.valueOf(ACCOUNT_ONE.hashCode());
        assertEquals(accountFirst.getSortId(),expectedSortedFirstId);

        Account accountSecond = new Account(ACCOUNT_TWO);
        Long expectedSortedSecondId = Long.valueOf(ACCOUNT_TWO.hashCode());
        assertEquals(accountSecond.getSortId(),expectedSortedSecondId);

        Account accountThird = new Account(ACCOUNT_THREE);
        Long expectedSortedThirdId = Long.valueOf(ACCOUNT_THREE.hashCode());
        assertEquals(accountThird.getSortId(),expectedSortedThirdId);

        Account accountFour = new Account(ACCOUNT_FOUR);
        Long expectedSortedFourId = Long.valueOf(ACCOUNT_FOUR.hashCode());
        assertEquals(accountFour.getSortId(),expectedSortedFourId);

        assertTrue(accountFirst.getSortId()<accountSecond.getSortId());
        assertTrue(accountSecond.getSortId()<accountThird.getSortId());
        assertTrue(accountThird.getSortId()<accountFour.getSortId());
    }

    @Test    public void shouldReturnCorrectOrderOfAccountsForTwoAccounts() {
        Account accountFirst = new Account(ACCOUNT_ONE);
        Account accountSecond = new Account(ACCOUNT_TWO);
        List<Account> accounts = Arrays.asList(accountFirst,accountSecond);
        TransferServiceImpl transferService = new TransferServiceImpl(accountsService,notificationService);
        List<Account> sortedAccounts = transferService.getSortedAccountsBySortedId(accounts);

        assertTrue(accountFirst.getSortId()<accountSecond.getSortId());
        assertEquals(sortedAccounts.get(FIRST_LOCK), accountFirst);
        assertEquals(sortedAccounts.get(SECOND_LOCK), accountSecond);
    }

    @Test
    public void shouldReturnCorrectOrderOfAccountsForManyAccounts() {
        Account accountFirst = new Account(ACCOUNT_ONE);
        Account accountSecond = new Account(ACCOUNT_TWO);
        Account accountThird = new Account(ACCOUNT_THREE);
        Account accountFour = new Account(ACCOUNT_FOUR);
        List<Account> accounts = Arrays.asList(accountFirst,accountSecond,accountThird,accountFour);
        TransferServiceImpl transferService = new TransferServiceImpl(accountsService,notificationService);
        List<Account> sortedAccounts = transferService.getSortedAccountsBySortedId(accounts);

        assertTrue(accountFirst.getSortId()<accountSecond.getSortId());
        assertTrue(accountSecond.getSortId()<accountThird.getSortId());
        assertTrue(accountThird.getSortId()<accountFour.getSortId());

        assertEquals(sortedAccounts.get(FIRST_LOCK), accountFirst);
        assertEquals(sortedAccounts.get(SECOND_LOCK), accountSecond);
    }

    @Test
    public void verifyOrderOfLoksThenTransferFromAccountOneToAccountTwo() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_ONE)).thenReturn(ACCOUNT_1_BALANCE_10);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TWO)).thenReturn(ACCOUNT_2_BALANCE_0 );

        Mockito.when(accountsService.findAccountById(ACCOUNT_ONE)).thenReturn(ACCOUNT_1_BALANCE_10);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TWO)).thenReturn(ACCOUNT_2_BALANCE_0 );


        Transfer transfer = new Transfer(ACCOUNT_ONE, ACCOUNT_TWO, AMOUNT_10);
        transferService.transferMoneyLock(transfer);

        assertTrue(ACCOUNT_1_BALANCE_10.getSortId()<ACCOUNT_2_BALANCE_0.getSortId());

        List<Account> sortedAccount =
        transferService.getSortedAccountsBySortedId(Arrays.asList(ACCOUNT_1_BALANCE_10,ACCOUNT_2_BALANCE_0));

        assertEquals(ACCOUNT_1_BALANCE_10,sortedAccount.get(FIRST_LOCK));
        assertEquals(ACCOUNT_2_BALANCE_0,sortedAccount.get(SECOND_LOCK));

        Account lockedEveryFirst = sortedAccount.get(FIRST_LOCK);
        Account lockedEverySecond = sortedAccount.get(SECOND_LOCK);

        assertTrue(lockedEveryFirst.getSortId()<lockedEverySecond.getSortId());

        assertTrue(lockedEveryFirst.equals(ACCOUNT_1_BALANCE_10));
        assertTrue(lockedEverySecond.equals(ACCOUNT_2_BALANCE_0));

        InOrder orderVerifier = Mockito.inOrder(accountsService,notificationService);

        orderVerifier.verify(accountsService).makeTransfer(ACCOUNT_ONE, ACCOUNT_TWO,AMOUNT_10);
        orderVerifier.verify(notificationService,times(2)).notifyAboutTransfer(Mockito.any(), Mockito.any());

    }

    @Test
    public void verifyOrderOfLoksThenTransferFromAccountTwoToAccountOne() throws Exception {
        Mockito.when(accountsService.findAccountById(ACCOUNT_ONE)).thenReturn(ACCOUNT_1_BALANCE_0);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TWO)).thenReturn(ACCOUNT_2_BALANCE_10 );

        Mockito.when(accountsService.findAccountById(ACCOUNT_ONE)).thenReturn(ACCOUNT_1_BALANCE_0);
        Mockito.when(accountsService.findAccountById(ACCOUNT_TWO)).thenReturn(ACCOUNT_2_BALANCE_10 );


        Transfer transfer = new Transfer( ACCOUNT_TWO, ACCOUNT_ONE,AMOUNT_10);
        transferService.transferMoneyLock(transfer);

        assertTrue(ACCOUNT_1_BALANCE_0.getSortId()<ACCOUNT_2_BALANCE_10.getSortId());
        List<Account> sortedAccount =
                transferService.getSortedAccountsBySortedId(Arrays.asList(ACCOUNT_1_BALANCE_0,ACCOUNT_2_BALANCE_10));

        assertEquals(ACCOUNT_1_BALANCE_0,sortedAccount.get(FIRST_LOCK));
        assertEquals(ACCOUNT_2_BALANCE_10,sortedAccount.get(SECOND_LOCK));

        Account lockedEveryFirst = sortedAccount.get(FIRST_LOCK);
        Account lockedEverySecond = sortedAccount.get(SECOND_LOCK);

        assertTrue(lockedEveryFirst.getSortId()<lockedEverySecond.getSortId());
        assertTrue(lockedEveryFirst.equals(ACCOUNT_1_BALANCE_0));
        assertTrue(lockedEverySecond.equals(ACCOUNT_2_BALANCE_10));

        InOrder orderVerifier = Mockito.inOrder(accountsService,notificationService);

        orderVerifier.verify(accountsService).makeTransfer( ACCOUNT_TWO,ACCOUNT_ONE,AMOUNT_10);

        orderVerifier.verify(notificationService,times(2)).notifyAboutTransfer(Mockito.any(), Mockito.any());

    }
}

