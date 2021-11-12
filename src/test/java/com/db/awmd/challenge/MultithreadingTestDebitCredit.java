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
public class MultithreadingTestDebitCredit {
    private static final Integer NUMBER_OF_THREADS = 1400;
    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static final String ACCOUNT_NAME_1 = "acc-1";
    private static final String ACCOUNT_NAME_2 = "acc-2";

    private static final BigDecimal ACCOUNT_1_AMOUNT = BigDecimal.valueOf(20000);
    private static final BigDecimal ACCOUNT_2_AMOUNT = BigDecimal.valueOf(15000);

    private static final BigDecimal ACCOUNT_1_AMOUNT_RESULT = BigDecimal.valueOf(18000);
    private static final BigDecimal ACCOUNT_2_AMOUNT_RESULT = BigDecimal.valueOf(17000);

    private static final Integer NUMBER_OF_OPERATION_1 = 500;
    private static final Integer NUMBER_OF_OPERATION_2 = 200;
    private static final BigDecimal TRANSFER_AMOUNT_10 = BigDecimal.valueOf(10);
    private static final BigDecimal TRANSFER_AMOUNT_20 = BigDecimal.valueOf(20);


    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountsService accountsService;


    @Test
    public void runMultiplyThreadsForTransferService() throws Exception {
        Account account1 = new Account(ACCOUNT_NAME_1);
        Account account2 = new Account(ACCOUNT_NAME_2);
        accountsService.clearAccounts();
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        accountsService.creditBalanceAccount(ACCOUNT_NAME_1, ACCOUNT_1_AMOUNT);
        accountsService.creditBalanceAccount(ACCOUNT_NAME_2, ACCOUNT_2_AMOUNT);

        List<Callable<Transfer>> callables = new ArrayList<>();

        assertEquals(accountsService.getAccount(ACCOUNT_NAME_1).getBalance(), ACCOUNT_1_AMOUNT);
        assertEquals(accountsService.getAccount(ACCOUNT_NAME_2).getBalance(), ACCOUNT_2_AMOUNT);

        for (int i = 0; i < NUMBER_OF_OPERATION_1; i++) {
            callables.add(createCallable(ACCOUNT_NAME_1, ACCOUNT_NAME_2, TRANSFER_AMOUNT_10));
        }

        for (int i = 0; i < NUMBER_OF_OPERATION_2; i++) {
            callables.add(createCallable(ACCOUNT_NAME_2, ACCOUNT_NAME_1, TRANSFER_AMOUNT_20));
        }

        for (int i = 0; i < NUMBER_OF_OPERATION_1; i++) {
            callables.add(createCallable(ACCOUNT_NAME_1, ACCOUNT_NAME_2, TRANSFER_AMOUNT_10));
        }

        for (int i = 0; i < NUMBER_OF_OPERATION_2; i++) {
            callables.add(createCallable(ACCOUNT_NAME_2, ACCOUNT_NAME_1, TRANSFER_AMOUNT_20));
        }

        EXECUTORS.invokeAll(callables);

        assertEquals(accountsService.getAccount(ACCOUNT_NAME_1).getBalance(), ACCOUNT_1_AMOUNT_RESULT);
        assertEquals(accountsService.getAccount(ACCOUNT_NAME_2).getBalance(), ACCOUNT_2_AMOUNT_RESULT);

    }

    private Callable<Transfer> createCallable(String accountFrom, String accountTo, BigDecimal amount) {
        Callable<Transfer> callableTask = () -> {
            Transfer transfer = new Transfer(accountFrom, accountTo, amount);
              transferService.transferMoneyLock(transfer);
              return  transfer;
        };
        return callableTask;
    }
}
