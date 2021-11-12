package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotExistException;
import com.db.awmd.challenge.exception.AccountNotProcessedExeption;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.OverDraftException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class AccountsServiceImpl implements AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    @Autowired
    public AccountsServiceImpl(AccountsRepository accountsRepository) throws AccountNotExistException {
        this.accountsRepository = accountsRepository;
    }
    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        this.accountsRepository.createAccount(account);
    }
    @Override
    public List<Account> getAllAccount() {
        return this.accountsRepository.getAllAccount();
    }

    @Override
    public Account getAccount(String accountId) {
        return accountsRepository.getAccount(accountId);
    }

    public void clearAccounts() {
        accountsRepository.clearAccounts();
    }


    @Transactional
    public Account findAccountById(String accountId) throws AccountNotExistException {
        Account account = getAccount(accountId);
        if (account == null) {
            throw new AccountNotExistException("Account with id " + accountId + " does not exists");
        }
        return account;
    }

    @Transactional
    public void updateAccount(Account account) throws AccountNotExistException {
        accountsRepository.updateAccount(account);
    }

    @Transactional
    public void creditBalanceAccount(String accountId, BigDecimal amount) throws AccountNotExistException ,AccountNotProcessedExeption{
        try {
            Account accountCredit = findAccountById(accountId);
            accountCredit.setBalance(accountCredit.getBalance().add(amount));
            accountsRepository.updateAccount(accountCredit);
        } catch(AccountNotExistException e) {
            log.error("Cannot process credit operation for accountId:"+accountId);
            throw e;
        }catch (Exception e) {
            log.error("Cannot process credit operation for accountId:"+accountId);
            throw new AccountNotProcessedExeption("Cannot process payment");
        }
    }

    @Transactional
    public void debitBalanceAccount(String accountId, BigDecimal amount) throws AccountNotExistException,AccountNotProcessedExeption, OverDraftException {
        try {
            Account accountDebit = findAccountById(accountId);
            if (accountDebit.getBalance().compareTo(amount) < 0) {
                throw new OverDraftException("Account with id:"+accountId+" does not have enough monney for withdraw.");
            }
            accountDebit.setBalance(accountDebit.getBalance().subtract(amount));
            accountsRepository.updateAccount(accountDebit);
        } catch(AccountNotExistException | OverDraftException e) {
            log.error("Cannot process debit operation for accountId:"+accountId);
            throw e;
        }catch (Exception e) {
            log.error("Cannot process debit operation for accountId:"+accountId);
            throw new AccountNotProcessedExeption("Cannot process payment");
        }

    }

    @Transactional
    public void makeTransfer(String accountFromId,String accountToId, BigDecimal amount) throws
            AccountNotExistException,AccountNotProcessedExeption, OverDraftException {
        try{
            debitBalanceAccount(accountFromId,amount);
        } catch (Exception e) {
            log.error("Cannot process debit operation for accountId.Transfer canceled.");
            throw e;
        }
        Boolean isCreditOperationSuccess = false;

        try {
            creditBalanceAccount(accountToId, amount);
            isCreditOperationSuccess = true;
        } catch (Exception e) {
            log.error("Cannot process credit operation for account.Transfer canceled.");
            throw e;
        }

        if (!isCreditOperationSuccess) {
            try {
                creditBalanceAccount(accountFromId, amount);
            } catch (Exception e) {
                log.error("Cannot process rollback operation for accountid"+accountFromId+".Transfer canceled.");
                throw new AccountNotProcessedExeption("Cannot process transfer");
            }
        }


    }

}
