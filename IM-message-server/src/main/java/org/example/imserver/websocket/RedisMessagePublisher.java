package org.example.imserver.websocket;

import com.example.domain.dto.ChatMessage;
import com.google.gson.Gson;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

/*
*@author Junjie
*@version 1.0
*@date 2025-11-14
 */
@Service
public class RedisMessagePublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;
    private final Gson gson=new Gson();

    public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate, ChannelTopic topic) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    public void publish(ChatMessage message){
        String messageJson=gson.toJson(message);
        redisTemplate.convertAndSend(topic.getTopic(), messageJson);
    }
}
