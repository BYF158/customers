package com.example.visit.service;

import com.example.visit.common.BizException;
import com.example.visit.dto.Dtos.GroupReq;
import com.example.visit.dto.Views.GroupView;
import com.example.visit.entity.CustomerGroup;
import com.example.visit.repo.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groups;

    public GroupService(GroupRepository groups) {
        this.groups = groups;
    }

    public List<GroupView> list(String keyword) {
        return groups.findAll(keyword);
    }

    public CustomerGroup get(long id) {
        return groups.findById(id).orElseThrow(() -> BizException.notFound("集团不存在"));
    }

    public long create(GroupReq req) {
        String code = req.code().trim();
        String name = req.name().trim();
        if (groups.existsCode(code, null)) {
            throw BizException.conflict("集团编码已存在：" + code);
        }
        return groups.insert(code, name);
    }

    public void update(long id, GroupReq req) {
        CustomerGroup group = get(id);
        String code = req.code().trim();
        String name = req.name().trim();
        if (groups.existsCode(code, group.id())) {
            throw BizException.conflict("集团编码已存在：" + code);
        }
        groups.update(id, code, name);
    }

    public void delete(long id) {
        get(id);
        groups.delete(id);
    }
}
