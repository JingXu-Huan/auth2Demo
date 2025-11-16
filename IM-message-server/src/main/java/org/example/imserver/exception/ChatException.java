package org.example.imserver.exception;

import lombok.Getter;

/**
 * 聊天相关异常
 * @author Junjie
 * @date 2025-11-13
 */
@Getter
public class ChatException extends RuntimeException {
    private final String code;

    public ChatException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ChatException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}
