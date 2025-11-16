package com.example.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Junjie
 * @version 1.0
 *  @date 2025-11-13
 * 消息dto,包含发送接收方id,群组id(仅在channelType为group时处理),payload为消息内容载体
 */

@Data
@NoArgsConstructor
public class ChatMessage {

    /*
    发送方id
     */
    private String senderId;

    /*

    接收方id
     */

    private String receiverId;

    /*
    群组id
     */

    private String groupId;

    /*
    信道类型,主要为了区分群组和单发消息类型
     */
    private ChannelType channelType;

    /*
    消息类型,区分文本,图像等等消息类型
     */
    private ContentType contentType;

    /*
    消息的实际载体
     */
    private Object payload;


    /*
    信道枚举
     */
    public enum ChannelType {
        PRIVATE,
        GROUP
    }

    /*
    消息内容枚举
     */
    public enum ContentType {
        TEXT,
        IMAGE,
        VIDEO,
        FILE,
        AUDIO,
        SYSTEM
    }

    /**
     * 文本载体
     */
    @Data
    @NoArgsConstructor
    public static class TextPayload {
        private String text;
        public TextPayload(String text) { this.text = text; }
    }

    /**
     * 图片载体
     */
    @Data
    @NoArgsConstructor
    public static class ImagePayload {
        //图片存储在服务器的url
        private String url;
        //文件名
        private String filename;
        //图片大小
        private long sizeInBytes;
    }

    /**
     * 文件载体
     */
    @Data
    @NoArgsConstructor
    public static class FilePayload {
        //文件存储在服务器的url
        private String url;
        //文件名
        private String fileName;
        //文件大小
        private long sizeInBytes;
        //文件扩展名
        private String fileExtension;
    }

    /**
     * 视频载体
     */
    @Data
    @NoArgsConstructor
    public static class VideoPayload {
        //影像存储在服务器的位置
        private String url;
        //首页图的存储位置
        private String thumbnailUrl;
        //影像时长
        private long duration;
        //影像大小
        private long sizeInBytes;
    }
}