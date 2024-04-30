package com.team1.moim.domain.chat.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;
import lombok.Getter;

@Getter
public class IsBeforeNowException extends MoimException {

    private static final ErrorCode errorcode = ErrorCode.IS_BEFORE_NOW;

    public IsBeforeNowException() {
        super(errorcode);
    }
}
