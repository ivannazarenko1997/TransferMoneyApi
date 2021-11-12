package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.*;

public interface TransferService {

    void transferMoneyLock(Transfer transfer) throws AccountNotProcessedExeption,
            TransferNotProcessException, OverDraftException,
            AccountNotExistException, SameOperationalAccountException;

}
