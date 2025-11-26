package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocPermissionMapper extends BaseMapper<DocPermission> {
    
    @Select("SELECT * FROM doc_permissions WHERE doc_id = #{docId}")
    List<DocPermission> selectByDocId(@Param("docId") Long docId);
    
    @Select("SELECT * FROM doc_permissions WHERE doc_id = #{docId} AND grantee_type = #{granteeType} AND grantee_id = #{granteeId}")
    DocPermission selectByGrantee(@Param("docId") Long docId, @Param("granteeType") String granteeType, @Param("granteeId") Long granteeId);
}
