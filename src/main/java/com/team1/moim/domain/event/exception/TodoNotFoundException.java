package com.team1.moim.domain.event.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;
import lombok.Getter;

@Getter
public class TodoNotFoundException extends MoimException {

    private static final ErrorCode errorCode = ErrorCode.TODO_NOT_FOUND;

    public TodoNotFoundException() {
        super(errorCode);
    }
}
