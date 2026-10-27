package com.example.visit.service;

import com.example.visit.common.BizException;
import com.example.visit.common.PageResult;
import com.example.visit.common.Periods;
import com.example.visit.dto.Dtos.PlanCreateReq;
import com.example.visit.dto.Dtos.PlanUpdateReq;
import com.example.visit.dto.Views.PlanView;
import com.example.visit.entity.CustomerGroup;
import com.example.visit.entity.User;
import com.example.visit.repo.GroupRepository;
import com.example.visit.repo.PlanRepository;
import com.example.visit.repo.PlanRepository.PlanOwner;
import com.example.visit.repo.UserRepository;
import com.example.visit.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository plans;
    private final UserRepository users;
    private final GroupRepository groups;

    public PlanService(PlanRepository plans, UserRepository users, GroupRepository groups) {
        this.plans = plans;
        this.users = users;
        this.groups = groups;
    }

    public PageResult<PlanView> search(CurrentUser current, Long managerId, String month, String status,
                                       String keyword, int page, int size) {
        Long scope = current.isAdmin() ? managerId : current.id();
        String monthFilter = (month == null || month.isBlank()) ? null : Periods.requireMonth(month);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return plans.search(scope, monthFilter, status, keyword, safePage, safeSize);
    }

    public PlanView get(CurrentUser current, long id) {
        PlanView plan = plans.findViewById(id).orElseThrow(() -> BizException.notFound("拜访计划不存在"));
        if (!current.isAdmin() && !plan.managerId().equals(current.id())) {
            throw BizException.forbidden("无权查看他人的拜访计划");
        }
        return plan;
    }

    public long create(PlanCreateReq req) {
        String month = Periods.requireMonth(req.planMonth());
        User manager = users.findById(req.managerId())
                .orElseThrow(() -> BizException.badRequest("客户经理不存在"));
        if (!User.ROLE_MANAGER.equals(manager.role())) {
            throw BizException.badRequest("只能给客户经理分配拜访计划");
        }
        CustomerGroup group = groups.findById(req.groupId())
                .orElseThrow(() -> BizException.badRequest("集团不存在"));
        if (plans.exists(manager.id(), group.id(), month, null)) {
            throw BizException.conflict("该客户经理在 " + month + " 已分配过「" + group.name() + "」");
        }
        return plans.insert(manager.id(), group.id(), month, trimToNull(req.remark()));
    }

    public void update(long id, PlanUpdateReq req) {
        PlanOwner owner = plans.findOwner(id).orElseThrow(() -> BizException.notFound("拜访计划不存在"));
        Long managerId = req.managerId() == null ? owner.managerId() : req.managerId();
        Long groupId = req.groupId() == null ? owner.groupId() : req.groupId();
        String month = (req.planMonth() == null || req.planMonth().isBlank())
                ? owner.planMonth() : Periods.requireMonth(req.planMonth());
        String remark = req.remark() == null ? null : trimToNull(req.remark());

        User manager = users.findById(managerId)
                .orElseThrow(() -> BizException.badRequest("客户经理不存在"));
        if (!User.ROLE_MANAGER.equals(manager.role())) {
            throw BizException.badRequest("只能分配给客户经理");
        }
        CustomerGroup group = groups.findById(groupId)
                .orElseThrow(() -> BizException.badRequest("集团不存在"));
        if (plans.exists(managerId, groupId, month, id)) {
            throw BizException.conflict("该客户经理在 " + month + " 已分配过「" + group.name() + "」");
        }
        plans.update(id, managerId, groupId, month, remark);
    }

    public void delete(long id) {
        plans.findOwner(id).orElseThrow(() -> BizException.notFound("拜访计划不存在"));
        plans.delete(id);
    }

    public List<String> months(CurrentUser current) {
        Long scope = current.isAdmin() ? null : current.id();
        return plans.distinctMonths(scope);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
