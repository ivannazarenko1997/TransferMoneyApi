package com.db.awmd.challenge.domain;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;


@Data
public class LockAccount {
    @NotNull
    @NotEmpty
    private final String accountId;

    @NotNull
    @Min(value = 0, message = "Initial balance must be positive.")
    private BigDecimal possibleBalance;

    public LockAccount(String accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.possibleBalance = amount;

    }


}



