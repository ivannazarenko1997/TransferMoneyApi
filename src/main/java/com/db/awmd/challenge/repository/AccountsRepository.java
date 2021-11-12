package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.util.List;

public interface AccountsRepository {

    void createAccount(Account account) throws DuplicateAccountIdException;

    void updateAccount(Account account) throws AccountNotExistException;

    Account getAccount(String accountId) throws AccountNotExistException;

    List<Account> getAllAccount();

    void clearAccounts();
}
