package com.example.securevault.mapper;

import com.example.securevault.dto.UserResponse;
import com.example.securevault.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
