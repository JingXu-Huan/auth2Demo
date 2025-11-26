package org.example.imsearchservice.ingest;

import java.time.Instant;

/**
 * 从 RocketMQ 消费的 IM 消息事件简化版。
 *
 * 实际可以由 IM-message-server 发送，字段与那边保持一致即可。
 */
public class MsgEvent {

    private String msgId;
    private Long channelId;
    private Long senderId;
    private String content;
    private Instant timestamp;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
