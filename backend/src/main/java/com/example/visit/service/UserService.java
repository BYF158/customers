package com.example.visit.service;

import com.example.visit.common.BizException;
import com.example.visit.dto.Dtos.CreateUserReq;
import com.example.visit.dto.Dtos.UpdateUserReq;
import com.example.visit.dto.ViewMapper;
import com.example.visit.dto.Views.ManagerView;
import com.example.visit.dto.Views.UserView;
import com.example.visit.entity.User;
import com.example.visit.repo.UserRepository;
import com.example.visit.security.CurrentUser;
import com.example.visit.security.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public List<ManagerView> listManagers() {
        return users.findManagers();
    }

    @Transactional
    public UserView create(CreateUserReq req) {
        String username = req.username().trim();
        String role = req.role().trim().toUpperCase();
        if (!User.ROLE_ADMIN.equals(role) && !User.ROLE_MANAGER.equals(role)) {
            throw BizException.badRequest("角色只能是 ADMIN 或 MANAGER");
        }
        if (users.findByUsername(username).isPresent()) {
            throw BizException.conflict("账号已存在：" + username);
        }
        long id = users.insert(username, req.realName().trim(), encoder.encode(req.password()), role, true);
        User user = users.findById(id).orElseThrow(() -> new BizException(500, "创建失败"));
        return ViewMapper.toView(user);
    }

    public void update(long id, UpdateUserReq req) {
        User user = users.findById(id).orElseThrow(() -> BizException.notFound("账号不存在"));
        users.updateBasic(user.id(), req.realName().trim(), req.enabled());
    }

    public void resetPassword(long id, String password) {
        User user = users.findById(id).orElseThrow(() -> BizException.notFound("账号不存在"));
        users.updatePassword(user.id(), encoder.encode(password));
    }

    @Transactional
    public void delete(long id) {
        CurrentUser current = UserContext.get();
        if (current.id() == id) {
            throw BizException.badRequest("不能删除当前登录账号");
        }
        User user = users.findById(id).orElseThrow(() -> BizException.notFound("账号不存在"));
        if (user.isAdmin()) {
            throw BizException.badRequest("管理者账号不允许删除");
        }
        users.delete(id);
    }

    public List<UserView> options() {
        return users.findManagers().stream()
                .map(m -> new UserView(m.id(), m.username(), m.realName(), m.role(), m.enabled(), m.createdAt()))
                .toList();
    }
}
