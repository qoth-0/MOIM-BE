package com.team1.moim.domain.group.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;

public class NoMoreGroupListException extends MoimException {
    private static final ErrorCode errorcode = ErrorCode.GROUP_LIST_NOT_FOUND;

    public NoMoreGroupListException() {
        super(errorcode);
    }
}
