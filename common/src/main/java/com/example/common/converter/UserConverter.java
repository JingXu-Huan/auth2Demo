package com.example.common.converter;

import com.example.common.dto.UserDTO;
import com.example.common.model.User;
import com.example.common.vo.UserRegisterVO;
import com.example.common.vo.UserVO;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户对象转换器
 * 负责 Model、DTO、VO 之间的转换
 */
public class UserConverter {
    
    /**
     * User Entity -> UserVO
     */
    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setEmail(user.getEmail());
        vo.setEmailVerified(user.getEmailVerified());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCreatedAt(user.getCreatedAt());
        
        return vo;
    }
    
    /**
     * UserDTO -> UserVO
     */
    public static UserVO dtoToVO(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        
        UserVO vo = new UserVO();
        vo.setId(dto.getId());
        vo.setUsername(dto.getUsername());
        vo.setDisplayName(dto.getDisplayName());
        vo.setEmail(dto.getEmail());
        vo.setEmailVerified(dto.getEmailVerified());
        vo.setAvatarUrl(dto.getAvatarUrl());
        vo.setCreatedAt(dto.getCreatedAt());
        
        return vo;
    }
    
    /**
     * User Entity -> UserRegisterVO
     */
    public static UserRegisterVO toRegisterVO(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserRegisterVO(
            user.getId(),
            user.getUsername(),
            user.getEmail()
        );
    }
}
