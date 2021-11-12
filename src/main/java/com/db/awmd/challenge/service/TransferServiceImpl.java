package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@Service
@Slf4j
public class TransferServiceImpl implements TransferService {
    private static final Integer COUNT_OF_LOCK_CHECKS = 1000;
    private static final Integer FIRST_LOCK = 0;
    private static final Integer SECOND_LOCK = 1;

    @Getter
    private final AccountsService accountsService;

    @Getter
    private final NotificationService notificationService;

    @Autowired
    public TransferServiceImpl(AccountsService accountsService,
                               NotificationService notificationService) {
        this.accountsService = accountsService;
        this.notificationService = notificationService;
    }


    private Boolean checkIfEnoughtMonneyOnAccount(String accountId, BigDecimal amount) {
        return (accountsService.findAccountById(accountId).getBalance().compareTo(amount) >= 0) ? true : false;
    }


    private void tryToSendMessageToRecipients(Transfer transfer) {
        try {
            log.info("Starting to send mails to recipients");
            String debitMessageUser = "Monney was send from your account to " + transfer.getAccountToId() + " in amount " + transfer.getAmount();
            Account debitAccount = accountsService.findAccountById(transfer.getAccountFromId());
            notificationService.notifyAboutTransfer(debitAccount, debitMessageUser);

            String creditMessageUser = "Your account was deposit from " + transfer.getAccountFromId() + " in amount " + transfer.getAmount();
            Account creditAccount = accountsService.findAccountById(transfer.getAccountToId());
            notificationService.notifyAboutTransfer(creditAccount, creditMessageUser);
        } catch (Exception e) {
            log.info("Error while sending mails to recipients from:" + transfer.getAccountFromId() + " and to" + transfer.getAccountToId(), e);
        }
    }

    private void checkPossibleTransferConditionsOrReturnException(Transfer transfer) throws
            AccountNotProcessedExeption, OverDraftException,
            AccountNotExistException, SameOperationalAccountException {
        try {
            if (BigDecimal.ZERO.compareTo(transfer.getAmount()) > 0) {
                log.error("Amount of transaction below zero");
                throw new AccountNotProcessedExeption("Amount less zero.");
            }
            if (transfer.getAccountFromId().equals(transfer.getAccountToId())) {
                log.error("Cannot make process between same account number " + transfer.getAccountToId());
                throw new SameOperationalAccountException("From and To accounts is the same " + transfer.getAccountFromId());
            }
            if (accountsService.findAccountById(transfer.getAccountFromId()) == null) {
                log.error("Account with id " + transfer.getAccountFromId() + " does not exists");
                throw new AccountNotExistException("Account with id " + transfer.getAccountFromId() + " does not exists");
            }
            if (accountsService.findAccountById(transfer.getAccountToId()) == null) {
                log.error("Account with id " + transfer.getAccountToId() + " does not exists");
                throw new AccountNotExistException("Account with id " + transfer.getAccountToId() + " does not exists");
            }
            verifyFundsSufficiency(transfer);
        } catch (AccountNotProcessedExeption | AccountNotExistException | SameOperationalAccountException | OverDraftException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error not process transfer exception", e);
            throw new TransferNotProcessException("Error not process transfer exception");
        }
    }

    @Override
    public void transferMoneyLock(Transfer transfer) throws AccountNotProcessedExeption, TransferNullObjectException,
            TransferNotProcessException, OverDraftException, AccountNotExistException, SameOperationalAccountException {
        try {
            log.info("Requested money transfer [{}]", transfer.toString());
            checkPossibleTransferConditionsOrReturnException(transfer);

            Account accountFrom = accountsService.findAccountById(transfer.getAccountFromId());
            Account accountTo = accountsService.findAccountById(transfer.getAccountToId());

            List<Account> sortedAccountsForLocks = getSortedAccountsBySortedId(Arrays.asList(accountFrom,accountTo));
            Account firstAccountLock = sortedAccountsForLocks.get(FIRST_LOCK);
            Account secondAccountLock = sortedAccountsForLocks.get(SECOND_LOCK);

            synchronized(firstAccountLock) {
                log.info("Lock for accountId " + firstAccountLock.getAccountId() + " obtained");
                synchronized(secondAccountLock) {
                    log.info("Lock for accountId " + secondAccountLock.getAccountId() + " obtained");
                    verifyFundsSufficiency(transfer);
                    accountsService.makeTransfer(accountFrom, accountTo, transfer.getAmount());
                  }
            }

            tryToSendMessageToRecipients(transfer);
        } catch (AccountNotProcessedExeption | TransferNullObjectException |
                TransferNotProcessException | OverDraftException |
                AccountNotExistException | SameOperationalAccountException e) {
            log.error("Cannot process transfer", e);
            throw e;
        } catch (Exception e) {
            log.error("Cannot process transfer", e);
            throw new TransferNotProcessException("Cannot process transfer");
        }
    }


    public List<Account> getSortedAccountsBySortedId(List<Account> accounts){
        Collections.sort(accounts, new Comparator<Account>() {
            public int compare(Account a1, Account a2) {
                return  a1.getSortId().compareTo(a2.getSortId());
            }
        });
        return accounts;
    }


    private void verifyFundsSufficiency(Transfer transfer) {
        if (!checkIfEnoughtMonneyOnAccount(transfer.getAccountFromId(), transfer.getAmount())) {
            log.error("Account with id:" + transfer.getAccountFromId() + " does not have enough monney.");
            throw new OverDraftException("Account with id:" + transfer.getAccountFromId() + " does not have enough monney for withdraw.");
        }
    }


}
