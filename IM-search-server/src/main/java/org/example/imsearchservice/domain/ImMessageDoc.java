package org.example.imsearchservice.domain;

import java.time.Instant;
import java.util.List;

/**
 * IM 消息在 Elasticsearch 中的文档结构（idx_im_messages）。
 */
public class ImMessageDoc {

    private String msgId;
    private Long channelId;
    private Long senderId;
    private String content;
    private List<Float> vector;
    private Instant createdAt;

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

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
