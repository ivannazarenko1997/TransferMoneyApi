package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.WrongFormatException;
import com.db.awmd.challenge.service.AccountsService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {
    private static final String NO_ACCOUNTS_EXISTS = "No account records exists";
    private static final String ERROR_PROCESS_RESPONCE = "Error while processing data:";
    private static final String EMPTY_ACCOUNT_TO_VALUE = "Account is empty";
    private static final String EMPTY_AMOUNT_VALUE = "Amount is empty";
    private static final String NOT_BIGDECIMAL_FORMAT = "Amount is not big decimal format";
    private static final String AMOUNT_LESS_THEN_ZERO = "Amount is less then zero";


    private final AccountsService accountsService;

    @Autowired
    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
        log.info("Creating account {}", account);

        try {
            this.accountsService.createAccount(account);
        } catch (DuplicateAccountIdException daie) {
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @GetMapping(path = "/{accountId}")
    @Async("threadPoolTaskExecutor")
    public Account getAccount(@PathVariable String accountId) {
        log.info("Retrieving account for id {}", accountId);

        log.info("Retrieving account for id {}", accountId);
        return this.accountsService.getAccount(accountId);

    }

    @GetMapping(path = "/createAccount/{accountId}")
    public ResponseEntity<Object> createEmptyAccount(@PathVariable String accountId) {
        log.info("Create account for id {}", accountId);
        if (checkIfEmpty(accountId)) {
            throw new RuntimeException("Account from is empty");
        }

        try {
            this.accountsService.createAccount(new Account(accountId));
            return new ResponseEntity<>(accountsService.findAccountById(accountId), HttpStatus.CREATED);
        } catch (DuplicateAccountIdException daie) {
            log.error(ERROR_PROCESS_RESPONCE, daie);
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception daie) {
            log.error(ERROR_PROCESS_RESPONCE, daie);
            return new ResponseEntity<>(ERROR_PROCESS_RESPONCE, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{accountId}/balances")
    @ApiOperation(value = "Get account balance by id", response = Account.class, produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Account not found with ID")})
    public BigDecimal getBalance(
            @ApiParam(value = "ID related to the account", required = true) @PathVariable String accountId) {
        return accountsService.findAccountById(accountId).getBalance();
    }

    @GetMapping("/all")
    @ApiOperation(value = "Get account balance by id", response = Account.class, produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Account not found with ID")})
    public ResponseEntity<Object> getAllAccounts() {
        log.info("Retrieving all accounts");
        if (accountsService.getAllAccount().isEmpty()) {
            return new ResponseEntity<>(NO_ACCOUNTS_EXISTS, HttpStatus.CREATED);
        }
        return new ResponseEntity<>(accountsService.getAllAccount(), HttpStatus.CREATED);
    }

    @GetMapping("/clear")
    @ApiOperation(value = "Get account balance by id", response = Account.class, produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Account not found with ID")})
    public ResponseEntity<Object> clearAccounts() {
        log.info("Clear all accounts");
        accountsService.clearAccounts();
        return new ResponseEntity<>("Accounts clear", HttpStatus.CREATED);
    }

    @GetMapping("/{accountId}/{amount}/balance/add")
    @ApiOperation(value = "Get account balance by id", response = Account.class, produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Account not found with ID"),
            @ApiResponse(code = 404, message = "Account not found with ID")})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addBalance(
            @ApiParam(value = "Account id for add balance", required = true) @PathVariable String accountId,
            @ApiParam(value = "Amount add to balance", required = true) @PathVariable String amount) {
        log.info("Add balance {} to account {}", amount, accountId);

        validateIncomeAccountId(accountId);
        validateIncomeAmount(amount);
        try {
            Account account = accountsService.findAccountById(accountId);
            accountsService.creditBalanceAccount(account, new BigDecimal(amount));
            return new ResponseEntity<>(accountsService.findAccountById(accountId), HttpStatus.CREATED);
        } catch ( Exception daie) {
            log.error("Cannot make deposit payment for accountId:" + accountId);
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.CREATED);
        }

    }

    @GetMapping("/{accountId}/{amount}/balance/withdraw")
    @ApiOperation(value = "Withdraw balance by id", response = Account.class, produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Account not found with ID"),
            @ApiResponse(code = 404, message = "Account not found with ID")})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> withdrawBalance
            (@ApiParam(value = "Account id for add balance", required = true) @PathVariable String accountId,
             @ApiParam(value = "Amount add to balance", required = true) @PathVariable String amount) {
        log.info("Withdraw balance {} from account {}", amount, accountId);

        validateIncomeAccountId(accountId);
        validateIncomeAmount(amount);

        try {
            Account account = accountsService.findAccountById(accountId);
            accountsService.debitBalanceAccount(account,new BigDecimal(amount));
            return new ResponseEntity<>(accountsService.getAccount(accountId), HttpStatus.CREATED);
        } catch ( Exception daie) {
            log.error("Cannot make withdraw payment for accountId=" + accountId);
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.CREATED);
        }
    }

    private void validateIncomeAmount(String incomeAmount) {
        if (checkIfEmpty(incomeAmount)) {
            throw new WrongFormatException(EMPTY_AMOUNT_VALUE);
        }

        if (!checkIfBigDecimalValue(incomeAmount)) {
            throw new WrongFormatException(NOT_BIGDECIMAL_FORMAT);
        }

        if (new BigDecimal(incomeAmount).compareTo(BigDecimal.ZERO) < 0) {
            throw new WrongFormatException(AMOUNT_LESS_THEN_ZERO);
        }
    }

    private Boolean checkIfBigDecimalValue(String data) {
        try {
            new BigDecimal(data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void validateIncomeAccountId(String accountId) {
        if (checkIfEmpty(accountId)) {
            throw new WrongFormatException(EMPTY_ACCOUNT_TO_VALUE);
        }
    }

    private Boolean checkIfEmpty(String data) {
        return (data != null && data.isEmpty()) ? true : false;
    }

}
