package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.UserStorageQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserStorageQuotaMapper extends BaseMapper<UserStorageQuota> {
    
    @Update("UPDATE user_storage_quotas SET used_storage = used_storage + #{size}, " +
            "file_count = file_count + 1 WHERE user_id = #{userId}")
    int incrementUsage(@Param("userId") Long userId, @Param("size") Long size);
    
    @Update("UPDATE user_storage_quotas SET used_storage = used_storage - #{size}, " +
            "file_count = file_count - 1 WHERE user_id = #{userId}")
    int decrementUsage(@Param("userId") Long userId, @Param("size") Long size);
}
