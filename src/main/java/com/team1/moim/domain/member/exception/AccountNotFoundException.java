package com.team1.moim.domain.member.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;
import lombok.Getter;

@Getter
public class AccountNotFoundException extends MoimException {

    private static final ErrorCode errorCode = ErrorCode.ACCOUNT_NOT_FOUND;

    public AccountNotFoundException(){
        super(errorCode);
    }
}
