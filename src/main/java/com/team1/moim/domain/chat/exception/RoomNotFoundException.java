package com.team1.moim.domain.chat.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;
import lombok.Getter;

@Getter
public class RoomNotFoundException extends MoimException {

    private static final ErrorCode errorCode = ErrorCode.ROOM_NOT_FOUND;
    public RoomNotFoundException() {
        super(errorCode);
    }
}
