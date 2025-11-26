package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocSpace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文档空间Mapper
 */
@Mapper
public interface DocSpaceMapper extends BaseMapper<DocSpace> {

    @Select("SELECT * FROM doc_spaces WHERE owner_id = #{ownerId} ORDER BY created_at DESC")
    List<DocSpace> findByOwner(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM doc_spaces WHERE team_id = #{teamId} ORDER BY created_at DESC")
    List<DocSpace> findByTeam(@Param("teamId") Long teamId);

    @Update("UPDATE doc_spaces SET doc_count = doc_count + 1 WHERE id = #{spaceId}")
    int incrementDocCount(@Param("spaceId") Long spaceId);

    @Update("UPDATE doc_spaces SET doc_count = doc_count - 1 WHERE id = #{spaceId} AND doc_count > 0")
    int decrementDocCount(@Param("spaceId") Long spaceId);
}
