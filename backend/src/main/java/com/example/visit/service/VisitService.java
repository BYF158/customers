package com.example.visit.service;

import com.example.visit.common.BizException;
import com.example.visit.common.PageResult;
import com.example.visit.dto.Dtos.VisitCreateReq;
import com.example.visit.dto.Dtos.VisitUpdateReq;
import com.example.visit.dto.Views.PlanView;
import com.example.visit.dto.Views.VisitView;
import com.example.visit.repo.PlanRepository;
import com.example.visit.repo.VisitRepository;
import com.example.visit.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VisitService {

    public static final List<String> METHODS = List.of("上门拜访", "电话沟通", "视频会议", "客户到访");
    public static final List<String> RESULTS = List.of("拜访顺利", "初步意向", "需再次跟进", "暂无意向", "未见到负责人");

    private final VisitRepository visits;
    private final PlanRepository plans;

    public VisitService(VisitRepository visits, PlanRepository plans) {
        this.visits = visits;
        this.plans = plans;
    }

    public PageResult<VisitView> search(CurrentUser current, Long managerId, String month,
                                        String keyword, int page, int size) {
        Long scope = current.isAdmin() ? managerId : current.id();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return visits.search(scope, month, keyword, safePage, safeSize);
    }

    @Transactional
    public long create(CurrentUser current, VisitCreateReq req) {
        PlanView plan = plans.findViewById(req.planId())
                .orElseThrow(() -> BizException.notFound("拜访计划不存在"));
        if (!current.isAdmin() && !plan.managerId().equals(current.id())) {
            throw BizException.forbidden("不能登记他人的拜访计划");
        }
        if (visits.existsByPlan(plan.id())) {
            throw BizException.conflict("该计划已登记过拜访，可在详情中修改或撤销");
        }
        String method = requireIn(req.method(), METHODS, "拜访方式");
        String result = requireIn(req.result(), RESULTS, "拜访结果");
        LocalDate date = req.visitDate();
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw BizException.badRequest("拜访日期不能晚于明天");
        }
        return visits.insert(plan.id(), date, method, result, trimToNull(req.remark()), current.id());
    }

    @Transactional
    public void update(CurrentUser current, long id, VisitUpdateReq req) {
        VisitView visit = visits.findById(id).orElseThrow(() -> BizException.notFound("拜访记录不存在"));
        assertScope(current, visit);
        LocalDate date = req.visitDate() == null ? visit.visitDate() : req.visitDate();
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw BizException.badRequest("拜访日期不能晚于明天");
        }
        String method = req.method() == null || req.method().isBlank()
                ? visit.method() : requireIn(req.method(), METHODS, "拜访方式");
        String result = req.result() == null || req.result().isBlank()
                ? visit.result() : requireIn(req.result(), RESULTS, "拜访结果");
        String remark = req.remark() == null ? visit.remark() : trimToNull(req.remark());
        visits.update(id, date, method, result, remark);
    }

    @Transactional
    public void delete(CurrentUser current, long id) {
        VisitView visit = visits.findById(id).orElseThrow(() -> BizException.notFound("拜访记录不存在"));
        assertScope(current, visit);
        visits.delete(id);
    }

    private void assertScope(CurrentUser current, VisitView visit) {
        if (!current.isAdmin() && !visit.managerId().equals(current.id())) {
            throw BizException.forbidden("不能操作他人的拜访记录");
        }
    }

    private String requireIn(String value, List<String> allowed, String label) {
        if (value == null || !allowed.contains(value.trim())) {
            throw BizException.badRequest(label + "只能是：" + String.join("、", allowed));
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
