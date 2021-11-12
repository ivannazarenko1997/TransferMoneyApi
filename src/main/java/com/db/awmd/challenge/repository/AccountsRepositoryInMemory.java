package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Integer INITIAL_CAPACITY = 32;
    private final Float LOAD_FACTOR = 0.75f;
    private final Integer CONCURENCY_LEVEL = 64;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>(INITIAL_CAPACITY, LOAD_FACTOR, CONCURENCY_LEVEL);

    private ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);

        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public void updateAccount(Account account) throws AccountNotExistException {
        if (account.getAccountId() == null) {
            throw new AccountNotExistException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
        accounts.put(account.getAccountId(), account);
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public List<Account> getAllAccount() {
        return new ArrayList<>(this.accounts.values());
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

}
