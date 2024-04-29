package com.team1.moim.domain.chat.exception;

import com.team1.moim.global.exception.ErrorCode;
import com.team1.moim.global.exception.MoimException;
import lombok.Getter;

@Getter
public class HostChatIncludedException extends MoimException {

    private static final ErrorCode errorCode = ErrorCode.HOST_CHAT_INCLUDED;

    public HostChatIncludedException() {
        super(errorCode);
    }
}
