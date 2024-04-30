package com.team1.moim.domain.chat.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;
import lombok.Getter;

@Getter
public class NoChatParticipantsException extends MoimException {

    private static final ErrorCode errorCode = ErrorCode.NO_CHAT_PARTICIPANT;

    public NoChatParticipantsException() {
        super(errorCode);
    }

}
