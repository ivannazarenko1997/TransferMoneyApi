package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MultithreadingTestWithdrawDepositTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountsService accountsService;
    private static final Integer NUMBER_OF_THREADS= 1000;

    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static final String ACCOUNT_NAME_1 = "acc-1";
    private static final String ACCOUNT_NAME_2 = "acc-2";

    private static final BigDecimal ACCOUNT_1_AMOUNT = BigDecimal.valueOf(40000);
    private static final BigDecimal ACCOUNT_2_AMOUNT = BigDecimal.valueOf(30000);

    private static final BigDecimal ACCOUNT_1_AMOUNT_RESULT = BigDecimal.valueOf(41000);
    private static final BigDecimal ACCOUNT_2_AMOUNT_RESULT = BigDecimal.valueOf(29000);

    private static final BigDecimal TRANSFER_AMOUNT_FROM_ONE_TO_TWO = BigDecimal.valueOf(1);
    private static final BigDecimal TRANSFER_AMOUNT_FROM_TWO_TO_ONE = BigDecimal.valueOf(2);
    private static final Integer NUMBER_OF_OPERATION = 1000;



    @Test
    public void runMultiplyThreadsForTransferService() throws Exception {
        Account account1 = new Account(ACCOUNT_NAME_1);
        Account account2 = new Account(ACCOUNT_NAME_2);
        accountsService.clearAccounts();
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        accountsService.creditBalanceAccount(account1, ACCOUNT_1_AMOUNT);
        accountsService.creditBalanceAccount(account2, ACCOUNT_2_AMOUNT);
        assertEquals(accountsService.getAccount(ACCOUNT_NAME_1).getBalance(), ACCOUNT_1_AMOUNT);
        assertEquals(accountsService.getAccount(ACCOUNT_NAME_2).getBalance(), ACCOUNT_2_AMOUNT);
        List<Callable<Transfer>> callables = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_OPERATION; i++) {

            Callable<Transfer> callableTaskTransferOneToTwo = () -> {
                Transfer transfer = new Transfer(ACCOUNT_NAME_1, ACCOUNT_NAME_2, TRANSFER_AMOUNT_FROM_ONE_TO_TWO);
                transferService.transferMoneyLock(transfer);
                return transfer;
            };

            Callable<Transfer> callableTaskTransferTwoToOne = () -> {
                Transfer transfer = new Transfer(ACCOUNT_NAME_2, ACCOUNT_NAME_1, TRANSFER_AMOUNT_FROM_TWO_TO_ONE);
                transferService.transferMoneyLock(transfer);
                return transfer;
            };
            callables.add(callableTaskTransferTwoToOne);
            callables.add(callableTaskTransferOneToTwo);
        }


        EXECUTORS.invokeAll(callables);

        assertEquals(ACCOUNT_1_AMOUNT_RESULT, accountsService.getAccount(ACCOUNT_NAME_1).getBalance());
        assertEquals(ACCOUNT_2_AMOUNT_RESULT, accountsService.getAccount(ACCOUNT_NAME_2).getBalance());
    }

}
