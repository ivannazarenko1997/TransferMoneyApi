package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TransferControllerTest {

    private MockMvc mockMvc;

    private static final String ACCOUNT_FROM = "acc-1";
    private static final String ACCOUNT_TO = "acc-2";
    private static final BigDecimal INITIAL_BALANCE_0 = new BigDecimal(0);
    private static final BigDecimal BALANCE_10 = new BigDecimal(10);
    private static final BigDecimal TRANSFER_VALUE_10 = new BigDecimal(10);
    private static final BigDecimal TRANSFER_VALUE_20 = new BigDecimal(20);
    private static final String OVERDRADF_MESSAGE = "Account with id:acc-1 does not have enough monney for withdraw.";
    private static final String ACCOUNT_1_NOT_EXISTS = "Account with id acc-1 does not exists";
    private static final String ACCOUNT_2_NOT_EXISTS = "Account with id acc-2 does not exists";

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
        accountsService.clearAccounts();
    }

    @Test
    public void processTransfer() throws Exception {
        Account account1 = new Account(ACCOUNT_FROM);
        Account account2 = new Account(ACCOUNT_TO);
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        assertThat(this.accountsService.getAccount(ACCOUNT_TO).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        accountsService.creditBalanceAccount(ACCOUNT_FROM, BALANCE_10);

        String exspected = "Success";
        this.mockMvc.perform(get("/v1/transfers/process/" + ACCOUNT_FROM + "/" + ACCOUNT_TO + "/" + TRANSFER_VALUE_10))
                .andExpect(status().isCreated())
                .andExpect(content().string(exspected));

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        assertThat(this.accountsService.getAccount(ACCOUNT_TO).getBalance()).isEqualTo(BALANCE_10);
    }

    @Test
    public void processTransferWithOverdraftException() throws Exception {
        Account account1 = new Account(ACCOUNT_FROM);
        Account account2 = new Account(ACCOUNT_TO);
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        assertThat(this.accountsService.getAccount(ACCOUNT_TO).getBalance()).isEqualTo(INITIAL_BALANCE_0);

        this.mockMvc.perform(get("/v1/transfers/process/" + ACCOUNT_FROM + "/" + ACCOUNT_TO + "/" + TRANSFER_VALUE_10))
                .andExpect(status().isCreated())
                .andExpect(content().string(OVERDRADF_MESSAGE));

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        assertThat(this.accountsService.getAccount(ACCOUNT_TO).getBalance()).isEqualTo(INITIAL_BALANCE_0);
    }

    @Test
    public void processTransferAndReturnNotExistSecondAccountMessage() throws Exception {
        accountsService.clearAccounts();
        Account account1 = new Account(ACCOUNT_FROM);
        accountsService.createAccount(account1);

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        this.mockMvc.perform(get("/v1/transfers/process/" + ACCOUNT_FROM + "/" + ACCOUNT_TO + "/" + TRANSFER_VALUE_10))
                .andExpect(status().isCreated())
                .andExpect(content().string(ACCOUNT_2_NOT_EXISTS));
    }

    @Test
    public void processTransferAndReturnNotExistAccountMessage() throws Exception {
        this.mockMvc.perform(get("/v1/transfers/process/" + ACCOUNT_FROM + "/" + ACCOUNT_TO + "/" + TRANSFER_VALUE_10))
                .andExpect(status().isCreated())
                .andExpect(content().string(ACCOUNT_1_NOT_EXISTS));
    }

    @Test
    public void addBalance() throws Exception {

        Account account1 = new Account(ACCOUNT_FROM);
        accountsService.clearAccounts();
        accountsService.createAccount(account1);

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
        String exspected = "{\"accountId\":\"acc-1\",\"balance\":10}";
        this.mockMvc.perform(get("/v1/accounts/" + ACCOUNT_FROM + "/" + TRANSFER_VALUE_10 + "/balance/add"))
                .andExpect(status().isCreated())
                .andExpect(content().string(exspected));
        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(TRANSFER_VALUE_10);

    }

    @Test
    public void withdrawBalance() throws Exception {

        Account account1 = new Account(ACCOUNT_FROM);

        accountsService.clearAccounts();
        accountsService.createAccount(account1);
        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);

        accountsService.creditBalanceAccount(ACCOUNT_FROM, TRANSFER_VALUE_10);

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(TRANSFER_VALUE_10);

        String exspected = "{\"accountId\":\"acc-1\",\"balance\":0}";
        this.mockMvc.perform(get("/v1/accounts/" + ACCOUNT_FROM + "/" + TRANSFER_VALUE_10 + "/balance/withdraw"))
                .andExpect(status().isCreated())
                .andExpect(content().string(exspected));
        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);
    }

    @Test
    public void tryToWithdrawBalanceAndObtainOverdraftMessage() throws Exception {

        Account account1 = new Account(ACCOUNT_FROM);

        accountsService.clearAccounts();
        accountsService.createAccount(account1);
        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);

        this.mockMvc.perform(get("/v1/accounts/" + ACCOUNT_FROM + "/" + TRANSFER_VALUE_20 + "/balance/withdraw"))
                .andExpect(status().isCreated())
                .andExpect(content().string(OVERDRADF_MESSAGE));
        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(INITIAL_BALANCE_0);

        accountsService.creditBalanceAccount(ACCOUNT_FROM, TRANSFER_VALUE_10);

        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(TRANSFER_VALUE_10);

        this.mockMvc.perform(get("/v1/accounts/" + ACCOUNT_FROM + "/" + TRANSFER_VALUE_20 + "/balance/withdraw"))
                .andExpect(status().isCreated())
                .andExpect(content().string(OVERDRADF_MESSAGE));
        assertThat(this.accountsService.getAccount(ACCOUNT_FROM).getBalance()).isEqualTo(TRANSFER_VALUE_10);
    }

}
