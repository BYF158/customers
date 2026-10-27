package com.example.visit.service;

import com.example.visit.common.BizException;
import com.example.visit.dto.Dtos.ChangePasswordReq;
import com.example.visit.dto.Dtos.LoginReq;
import com.example.visit.dto.ViewMapper;
import com.example.visit.dto.Views.LoginResult;
import com.example.visit.dto.Views.UserView;
import com.example.visit.entity.User;
import com.example.visit.repo.UserRepository;
import com.example.visit.security.CurrentUser;
import com.example.visit.security.JwtUtil;
import com.example.visit.security.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.users = users;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResult login(LoginReq req) {
        User user = users.findByUsername(req.username().trim())
                .orElseThrow(() -> BizException.unauthorized("账号或密码不正确"));
        if (!encoder.matches(req.password(), user.password())) {
            throw BizException.unauthorized("账号或密码不正确");
        }
        if (!user.enabled()) {
            throw BizException.forbidden("账号已停用，请联系管理者");
        }
        return new LoginResult(jwtUtil.generate(user), ViewMapper.toView(user));
    }

    public UserView me() {
        CurrentUser current = UserContext.get();
        User user = users.findById(current.id())
                .orElseThrow(() -> BizException.unauthorized("账号不存在，请重新登录"));
        return ViewMapper.toView(user);
    }

    public void changePassword(ChangePasswordReq req) {
        CurrentUser current = UserContext.get();
        User user = users.findById(current.id())
                .orElseThrow(() -> BizException.unauthorized("账号不存在，请重新登录"));
        if (!encoder.matches(req.oldPassword(), user.password())) {
            throw BizException.badRequest("当前密码不正确");
        }
        if (encoder.matches(req.newPassword(), user.password())) {
            throw BizException.badRequest("新密码不能与当前密码相同");
        }
        users.updatePassword(user.id(), encoder.encode(req.newPassword()));
    }
}
