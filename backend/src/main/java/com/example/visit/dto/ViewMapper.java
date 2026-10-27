package com.example.visit.dto;

import com.example.visit.dto.Views.UserView;
import com.example.visit.entity.User;

public final class ViewMapper {

    private ViewMapper() {
    }

    public static UserView toView(User user) {
        return new UserView(user.id(), user.username(), user.realName(), user.role(),
                user.enabled(), user.createdAt());
    }
}
