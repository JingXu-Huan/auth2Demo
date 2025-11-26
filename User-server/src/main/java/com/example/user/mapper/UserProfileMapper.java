package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户详情 Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
    
    /**
     * 根据用户ID获取详细信息
     */
    @Select("SELECT * FROM user_profiles WHERE user_id = #{userId}")
    UserProfile getByUserId(@Param("userId") Long userId);
    
    /**
     * 更新用户头像
     */
    @Update("UPDATE user_profiles SET avatar_url = #{avatarUrl}, updated_at = NOW() WHERE user_id = #{userId}")
    int updateAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);
    
    /**
     * 更新用户基本信息
     */
    @Update("UPDATE user_profiles SET " +
            "nickname = #{nickname}, " +
            "real_name = #{realName}, " +
            "gender = #{gender}, " +
            "birthday = #{birthday}, " +
            "bio = #{bio}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int updateBasicInfo(@Param("userId") Long userId,
                       @Param("nickname") String nickname,
                       @Param("realName") String realName,
                       @Param("gender") Integer gender,
                       @Param("birthday") String birthday,
                       @Param("bio") String bio);
    
    /**
     * 更新工作信息
     */
    @Update("UPDATE user_profiles SET " +
            "company = #{company}, " +
            "department = #{department}, " +
            "position = #{position}, " +
            "employee_id = #{employeeId}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int updateWorkInfo(@Param("userId") Long userId,
                      @Param("company") String company,
                      @Param("department") String department,
                      @Param("position") String position,
                      @Param("employeeId") String employeeId);
}
