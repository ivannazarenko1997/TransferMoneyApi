package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.*;
import com.db.awmd.challenge.service.TransferService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/v1/transfers")
@Slf4j
public class TransferController {
    private static final String EMPTY_ACCOUNT_TO_VALUE = "Account is empty";

    private static final String EMPTY_ACCOUNT_VALUE = "Account from is empty";
    private static final String EMPTY_AMOUNT_VALUE = "Amount is empty";
    private static final String NOT_BIGDECIMAL_FORMAT = "Amount is not big decimal format";
    private static final String AMOUNT_LESS_THEN_ZERO = "Amount is less then zero";
    private static final String CANNOT_PROCESS_PAYMENTS = "Cannot process payment";


    private final TransferService transferService;


    @Autowired
    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/process/{accountIdFrom}/{accountIdTo}/{amount}")
    @ApiOperation(value = "Transfer balance", response = Transfer.class, produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Transfer not processed"),
            @ApiResponse(code = 404, message = "Transfer not processed")})
    public ResponseEntity<Object> processTransfer(
            @ApiParam(value = "ID related From  account", required = true) @PathVariable String accountIdFrom,
            @ApiParam(value = "ID related To account", required = true) @PathVariable String accountIdTo,
            @ApiParam(value = "Amount", required = true) @PathVariable String amount) {
        log.info("Try to start transfer from accountIdFrom");

        validateIncomeAccountId(accountIdFrom);

        validateIncomeAccountId(accountIdTo);

        validateIncomeAmount(amount);

        try {
            Transfer transfer = new Transfer(accountIdFrom, accountIdTo, new BigDecimal(amount));
            transferService.transferMoneyLock(transfer);
            return new ResponseEntity<>("Success", HttpStatus.CREATED);
        } catch (AccountNotProcessedExeption | TransferNullObjectException |
                OverDraftException | AccountNotExistException | SameOperationalAccountException daie) {
            log.error("Cannot make transfer payment from " + accountIdFrom + " to " + accountIdTo + " amount " + amount);
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.CREATED);
        } catch (Exception daie) {
            log.error("Cannot make transfer payment from :" + accountIdFrom + " to " + accountIdTo + " amount " + amount + " ", daie);
            return new ResponseEntity<>(CANNOT_PROCESS_PAYMENTS, HttpStatus.CREATED);
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

    private void validateIncomeAccountId(String accountId) {
        if (checkIfEmpty(accountId)) {
            throw new WrongFormatException(EMPTY_ACCOUNT_TO_VALUE);
        }
    }

    private Boolean checkIfEmpty(String data) {
        return (data != null && data.isEmpty()) ? true : false;
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

}
