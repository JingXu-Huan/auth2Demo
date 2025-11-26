package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.ChannelSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChannelSequenceMapper extends BaseMapper<ChannelSequence> {
    
    @Select("SELECT get_next_seq_id(#{channelId})")
    Long getNextSeqId(@Param("channelId") Long channelId);
}
