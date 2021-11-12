package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.AccountNotProcessedExeption;
import com.db.awmd.challenge.exception.OverDraftException;

import java.math.BigDecimal;
import java.util.List;


public interface AccountsService {
    void createAccount(Account account);

    Account findAccountById(String accountId) throws AccountNotExistException;

    Account getAccount(String accountId);

    List<Account> getAllAccount();

    void creditBalanceAccount(Account account, BigDecimal amount) throws AccountNotExistException, AccountNotProcessedExeption;

    void debitBalanceAccount(Account account, BigDecimal amount) throws AccountNotExistException, AccountNotProcessedExeption, OverDraftException;

    void updateAccount(Account account) throws AccountNotExistException;

    void clearAccounts();

    void makeTransfer(Account accountFromId, Account accountToId, BigDecimal amount) throws AccountNotExistException, AccountNotProcessedExeption, OverDraftException;

}



