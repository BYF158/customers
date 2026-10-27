package com.example.visit.config;

import com.example.visit.entity.User;
import com.example.visit.repo.GroupRepository;
import com.example.visit.repo.PlanRepository;
import com.example.visit.repo.UserRepository;
import com.example.visit.repo.VisitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动初始化：管理者账号 + 演示数据（可用 INIT_DEMO=false 只建管理者）。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final GroupRepository groups;
    private final PlanRepository plans;
    private final VisitRepository visits;
    private final PasswordEncoder encoder;
    private final boolean initDemo;

    public DataInitializer(UserRepository users, GroupRepository groups, PlanRepository plans,
                           VisitRepository visits, PasswordEncoder encoder,
                           @Value("${app.init-demo:true}") boolean initDemo) {
        this.users = users;
        this.groups = groups;
        this.plans = plans;
        this.visits = visits;
        this.encoder = encoder;
        this.initDemo = initDemo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.findByUsername("admin").isPresent()) {
            return;
        }
        users.insert("admin", "管理员", encoder.encode("admin123"), User.ROLE_ADMIN, true);
        log.info("已创建默认管理者账号 admin / admin123，请登录后立即修改密码");

        if (!initDemo) {
            return;
        }

        String[] managerNames = {"小李", "小甘", "小郭"};
        String[] managerAccounts = {"xiaoli", "xiaogan", "xiaoguo"};
        Map<String, Long> managerIds = new LinkedHashMap<>();
        for (int i = 0; i < managerNames.length; i++) {
            long id = users.insert(managerAccounts[i], managerNames[i], encoder.encode("123456"),
                    User.ROLE_MANAGER, true);
            managerIds.put(managerNames[i], id);
        }

        String[][] groupData = {
                {"G001", "李明科技有限公司"},
                {"G002", "甘泉食品集团"},
                {"G003", "郭庄建材有限公司"},
                {"G004", "华东物流集团"},
                {"G005", "新时代电子科技"},
                {"G006", "恒达医药集团"},
                {"G007", "阳光新能源股份"},
                {"G008", "金桥机械设备"},
                {"G009", "盛世文化传媒"},
                {"G010", "汇通供应链管理"}
        };
        Map<String, Long> groupIds = new LinkedHashMap<>();
        for (String[] group : groupData) {
            groupIds.put(group[0], groups.insert(group[0], group[1]));
        }

        YearMonth now = YearMonth.now();
        String[] months = {now.toString(), now.plusMonths(1).toString(), now.plusMonths(2).toString()};
        // 与老版本一致的分配：每家集团一个计划月份
        Map<String, Integer> monthOfGroup = new LinkedHashMap<>();
        monthOfGroup.put("G001", 0);
        monthOfGroup.put("G002", 1);
        monthOfGroup.put("G003", 2);
        monthOfGroup.put("G004", 0);
        monthOfGroup.put("G005", 1);
        monthOfGroup.put("G006", 2);
        monthOfGroup.put("G007", 0);
        monthOfGroup.put("G008", 1);
        monthOfGroup.put("G009", 2);
        monthOfGroup.put("G010", 1);

        Map<String, Long> planIds = new LinkedHashMap<>();
        for (Map.Entry<String, Long> manager : managerIds.entrySet()) {
            for (Map.Entry<String, Integer> entry : monthOfGroup.entrySet()) {
                long planId = plans.insert(manager.getValue(), groupIds.get(entry.getKey()),
                        months[entry.getValue()], null);
                planIds.put(manager.getKey() + "|" + entry.getKey(), planId);
            }
        }

        // 当月已完成的拜访（让统计页有数据）
        String thisMonth = months[0];
        record Demo(String manager, String group, String method, String result, String remark, int daysAgo) {
        }
        List<Demo> demos = new ArrayList<>(List.of(
                new Demo("小李", "G001", "上门拜访", "初步意向", "与采购负责人沟通了年度供货计划，下月给方案", 5),
                new Demo("小李", "G004", "电话沟通", "拜访顺利", "确认了收货窗口时间，暂无新需求", 2),
                new Demo("小甘", "G001", "上门拜访", "需再次跟进", "对方在比价，需补充报价明细", 3),
                new Demo("小郭", "G004", "视频会议", "拜访顺利", "线上演示了系统，客户反馈良好", 1)
        ));
        for (Demo demo : demos) {
            Long planId = planIds.get(demo.manager() + "|" + demo.group());
            if (planId == null) {
                continue;
            }
            LocalDate date = LocalDate.now().minusDays(demo.daysAgo());
            visits.insert(planId, date.isAfter(LocalDate.now()) ? LocalDate.now() : date,
                    demo.method(), demo.result(), demo.remark(), managerIds.get(demo.manager()));
        }
        log.info("已初始化演示数据：{} 位客户经理、{} 家集团、{} 条拜访计划（当前月份 {}）",
                managerIds.size(), groupIds.size(), planIds.size(), thisMonth);
    }
}
