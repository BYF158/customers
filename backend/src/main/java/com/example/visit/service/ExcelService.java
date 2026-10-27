package com.example.visit.service;

import com.example.visit.common.BizException;
import com.example.visit.common.Periods;
import com.example.visit.dto.Dtos.ImportApplyReq;
import com.example.visit.dto.Dtos.ImportRow;
import com.example.visit.dto.Views.CreatedManager;
import com.example.visit.dto.Views.ImportPreview;
import com.example.visit.dto.Views.ImportResult;
import com.example.visit.dto.Views.ImportRowView;
import com.example.visit.dto.Views.PlanView;
import com.example.visit.dto.Views.VisitView;
import com.example.visit.entity.CustomerGroup;
import com.example.visit.entity.User;
import com.example.visit.repo.GroupRepository;
import com.example.visit.repo.PlanRepository;
import com.example.visit.repo.UserRepository;
import com.example.visit.repo.VisitRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Excel 导入导出：
 * - 导入：每个 Sheet = 一位客户经理，表头含“集团编码”“集团名称”与月份列（2026-10 / 十月 / 10月）
 * - 导出：拜访记录（明细 + 按集团汇总）、完整备份（可再导入的清单 + 拜访明细）、导入模板
 */
@Service
public class ExcelService {

    private static final Set<String> SKIP_SHEETS = Set.of("拜访明细", "按集团汇总", "导出说明", "说明");
    private static final Map<String, Integer> CN_MONTHS = new LinkedHashMap<>();
    private static final Pattern YEAR_MONTH = Pattern.compile("(\\d{4})\\s*[-/.年]\\s*(\\d{1,2})");
    private static final Pattern MONTH_ONLY = Pattern.compile("(\\d{1,2})\\s*月份?");

    static {
        String[] names = {"一月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "十二月"};
        for (int i = 0; i < names.length; i++) {
            CN_MONTHS.put(names[i], i + 1);
        }
    }

    private final UserRepository users;
    private final GroupRepository groups;
    private final PlanRepository plans;
    private final VisitRepository visits;
    private final PasswordEncoder encoder;
    private final String defaultPassword;

    public ExcelService(UserRepository users, GroupRepository groups, PlanRepository plans,
                        VisitRepository visits, PasswordEncoder encoder,
                        @Value("${app.default-password:123456}") String defaultPassword) {
        this.users = users;
        this.groups = groups;
        this.plans = plans;
        this.visits = visits;
        this.encoder = encoder;
        this.defaultPassword = defaultPassword;
    }

    /* ==================== 导入：预览 ==================== */

    public ImportPreview preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要导入的文件");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        try {
            if (filename.endsWith(".csv")) {
                return parseCsv(file);
            }
            try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
                return parseWorkbook(workbook);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.badRequest("文件解析失败：" + e.getMessage());
        }
    }

    private ImportPreview parseWorkbook(Workbook workbook) {
        List<ImportRowView> rows = new ArrayList<>();
        Set<String> managers = new LinkedHashSet<>();
        Set<String> months = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> nameByCode = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        int ignored = 0;

        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            String sheetName = workbook.getSheetName(index).trim();
            Sheet sheet = workbook.getSheetAt(index);
            if (sheetName.isEmpty() || SKIP_SHEETS.contains(sheetName) || sheetName.matches("(?i)sheet\\d*|工作表\\d*")) {
                ignored++;
                continue;
            }
            List<List<String>> grid = readGrid(sheet, formatter);
            int headerIndex = findHeaderRow(grid);
            if (headerIndex < 0) {
                warnings.add("工作表「" + sheetName + "」未找到包含“集团编码”的表头，已跳过");
                ignored++;
                continue;
            }
            List<String> header = grid.get(headerIndex);
            int codeIndex = -1;
            int nameIndex = -1;
            Map<Integer, String> monthColumns = new LinkedHashMap<>();
            for (int col = 0; col < header.size(); col++) {
                String text = header.get(col).trim();
                if (text.isEmpty()) {
                    continue;
                }
                if (codeIndex < 0 && (text.contains("编码") || text.equalsIgnoreCase("code"))
                        && !text.contains("集团名")) {
                    codeIndex = col;
                    continue;
                }
                if (nameIndex < 0 && (text.contains("名称") || text.contains("集团名") || text.contains("客户名"))
                        && !text.contains("编码")) {
                    nameIndex = col;
                    continue;
                }
                String month = parseMonthHeader(text);
                if (month != null) {
                    monthColumns.put(col, month);
                }
            }
            if (codeIndex < 0) {
                warnings.add("工作表「" + sheetName + "」缺少“集团编码”列，已跳过");
                ignored++;
                continue;
            }
            if (monthColumns.isEmpty()) {
                warnings.add("工作表「" + sheetName + "」没有识别到月份列（表头可写 2026-10 / 十月 / 10月），已跳过");
                ignored++;
                continue;
            }
            managers.add(sheetName);
            int validRows = 0;
            for (int r = headerIndex + 1; r < grid.size(); r++) {
                List<String> row = grid.get(r);
                String code = cell(row, codeIndex).trim();
                if (code.isEmpty()) {
                    continue;
                }
                String name = nameIndex >= 0 ? cell(row, nameIndex).trim() : "";
                if (name.isEmpty()) {
                    name = code;
                }
                String previousName = nameByCode.putIfAbsent(code, name);
                if (previousName != null && !previousName.equals(name)) {
                    warnings.add("集团编码 " + code + " 出现多个名称：" + previousName + " / " + name
                            + "，导入时以「" + previousName + "」为准");
                }
                boolean added = false;
                for (Map.Entry<Integer, String> entry : monthColumns.entrySet()) {
                    if (!cell(row, entry.getKey()).isBlank()) {
                        rows.add(new ImportRowView(sheetName, code,
                                previousName == null ? name : previousName, entry.getValue()));
                        months.add(entry.getValue());
                        added = true;
                    }
                }
                if (!added) {
                    String current = Periods.currentMonth();
                    warnings.add("「" + sheetName + "」第 " + (r + 1) + " 行 " + code
                            + " 未填任何月份，已按当前月份 " + current + " 处理");
                    rows.add(new ImportRowView(sheetName, code,
                            previousName == null ? name : previousName, current));
                    months.add(current);
                }
                validRows++;
            }
            if (validRows == 0) {
                warnings.add("工作表「" + sheetName + "」没有数据行");
            }
        }
        return buildPreview(rows, managers, months, warnings, ignored);
    }

    private ImportPreview parseCsv(MultipartFile file) {
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw BizException.badRequest("文件读取失败：" + e.getMessage());
        }
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> grid = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            grid.add(parseCsvLine(line));
        }
        List<String> warnings = new ArrayList<>();
        List<ImportRowView> rows = new ArrayList<>();
        Set<String> managers = new LinkedHashSet<>();
        Set<String> months = new LinkedHashSet<>();
        int headerIndex = findHeaderRow(grid);
        if (headerIndex < 0) {
            throw BizException.badRequest("CSV 未找到包含“集团编码”的表头");
        }
        List<String> header = grid.get(headerIndex);
        int codeIndex = -1;
        int nameIndex = -1;
        int managerIndex = -1;
        Map<Integer, String> monthColumns = new LinkedHashMap<>();
        for (int col = 0; col < header.size(); col++) {
            String text2 = header.get(col).trim();
            if (codeIndex < 0 && text2.contains("编码")) {
                codeIndex = col;
            } else if (managerIndex < 0 && (text2.contains("客户经理") || text2.equals("经理"))) {
                managerIndex = col;
            } else if (nameIndex < 0 && text2.contains("名称")) {
                nameIndex = col;
            } else {
                String month = parseMonthHeader(text2);
                if (month != null) {
                    monthColumns.put(col, month);
                }
            }
        }
        if (codeIndex < 0 || managerIndex < 0) {
            throw BizException.badRequest("CSV 表头需要包含“客户经理”和“集团编码”两列");
        }
        if (monthColumns.isEmpty()) {
            throw BizException.badRequest("CSV 表头没有识别到月份列（可写 2026-10 / 十月 / 10月）");
        }
        for (int r = headerIndex + 1; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            String manager = cell(row, managerIndex).trim();
            String code = cell(row, codeIndex).trim();
            if (manager.isEmpty() || code.isEmpty()) {
                continue;
            }
            String name = nameIndex >= 0 ? cell(row, nameIndex).trim() : code;
            if (name.isEmpty()) {
                name = code;
            }
            managers.add(manager);
            for (Map.Entry<Integer, String> entry : monthColumns.entrySet()) {
                if (!cell(row, entry.getKey()).isBlank()) {
                    rows.add(new ImportRowView(manager, code, name, entry.getValue()));
                    months.add(entry.getValue());
                }
            }
        }
        return buildPreview(rows, managers, months, warnings, 0);
    }

    private ImportPreview buildPreview(List<ImportRowView> rows, Set<String> managers, Set<String> months,
                                       List<String> warnings, int ignored) {
        // 去重：同一客户经理 + 集团 + 月份只保留一条
        Map<String, ImportRowView> unique = new LinkedHashMap<>();
        for (ImportRowView row : rows) {
            unique.putIfAbsent(row.managerName() + "|" + row.code() + "|" + row.planMonth(), row);
        }
        List<ImportRowView> result = new ArrayList<>(unique.values());
        if (result.isEmpty()) {
            throw BizException.badRequest("没有解析到有效数据，请检查表头是否包含“集团编码”和月份列");
        }
        List<String> monthList = months.stream().sorted().toList();
        return new ImportPreview(result, new ArrayList<>(managers), monthList, warnings, ignored);
    }

    /* ==================== 导入：写入 ==================== */

    @Transactional
    public ImportResult apply(ImportApplyReq req) {
        List<ImportRow> input = req.rows() == null ? List.of() : req.rows();
        if (input.isEmpty()) {
            throw BizException.badRequest("没有可导入的数据");
        }
        boolean replace = "REPLACE".equalsIgnoreCase(req.mode());
        List<String> warnings = new ArrayList<>();
        List<CreatedManager> createdManagers = new ArrayList<>();

        // 1. 规范化 + 去重
        record Normalized(String managerName, String code, String name, String month) {
        }
        Map<String, Normalized> unique = new LinkedHashMap<>();
        for (ImportRow row : input) {
            String manager = trim(row.managerName());
            String code = trim(row.code());
            String name = trim(row.name());
            String month = Periods.normalize(row.planMonth());
            if (manager.isEmpty() || code.isEmpty()) {
                warnings.add("跳过一条缺少客户经理或集团编码的数据");
                continue;
            }
            if (name.isEmpty()) {
                name = code;
            }
            if (month == null) {
                warnings.add("「" + manager + " / " + code + "」月份无法识别，已跳过");
                continue;
            }
            unique.putIfAbsent(manager + "|" + code + "|" + month, new Normalized(manager, code, name, month));
        }
        if (unique.isEmpty()) {
            throw BizException.badRequest("没有可导入的有效数据");
        }

        // 2. 客户经理：找到或自动创建
        Map<String, Long> managerIds = new LinkedHashMap<>();
        for (Normalized row : unique.values()) {
            if (managerIds.containsKey(row.managerName())) {
                continue;
            }
            Optional<User> existing = users.findManagerByName(row.managerName());
            if (existing.isPresent()) {
                managerIds.put(row.managerName(), existing.get().id());
            } else {
                String username = uniqueUsername(row.managerName());
                long id = users.insert(username, row.managerName(), encoder.encode(defaultPassword),
                        User.ROLE_MANAGER, true);
                managerIds.put(row.managerName(), id);
                createdManagers.add(new CreatedManager(row.managerName(), username, defaultPassword));
            }
        }

        // 3. 全量替换：只清理“本次导入涉及的客户经理 + 月份”范围，避免误删无关数据
        Set<String> involvedMonths = new LinkedHashSet<>();
        unique.values().forEach(r -> involvedMonths.add(r.month()));
        if (replace) {
            plans.deleteByManagersAndMonths(new ArrayList<>(managerIds.values()), new ArrayList<>(involvedMonths));
        }

        // 4. 集团：按编码 upsert（名称以第一次出现的为准，冲突仅提示）
        Map<String, Long> groupIds = new LinkedHashMap<>();
        Map<String, String> groupNames = new LinkedHashMap<>();
        for (Normalized row : unique.values()) {
            if (groupNames.containsKey(row.code())) {
                continue;
            }
            Optional<CustomerGroup> existing = groups.findByCode(row.code());
            if (existing.isPresent()) {
                CustomerGroup group = existing.get();
                groupIds.put(row.code(), group.id());
                groupNames.put(row.code(), group.name());
                if (!group.name().equals(row.name())) {
                    warnings.add("集团 " + row.code() + " 已存在，名称保持为「" + group.name()
                            + "」（表格中为「" + row.name() + "」）");
                }
            } else {
                long id = groups.insert(row.code(), row.name());
                groupIds.put(row.code(), id);
                groupNames.put(row.code(), row.name());
            }
        }

        // 5. 计划：存在则跳过
        int added = 0;
        int skipped = 0;
        for (Normalized row : unique.values()) {
            Long managerId = managerIds.get(row.managerName());
            Long groupId = groupIds.get(row.code());
            if (managerId == null || groupId == null) {
                skipped++;
                continue;
            }
            if (plans.exists(managerId, groupId, row.month(), null)) {
                skipped++;
            } else {
                plans.insert(managerId, groupId, row.month(), null);
                added++;
            }
        }
        String modeText = replace ? "替换" : "合并";
        String message = "导入完成（" + modeText + "）：新增 " + added + " 条计划，跳过 " + skipped + " 条已存在记录";
        return new ImportResult(added, skipped, createdManagers, warnings, message);
    }

    private String uniqueUsername(String base) {
        String candidate = base.length() > 40 ? base.substring(0, 40) : base;
        int suffix = 1;
        while (users.findByUsername(candidate).isPresent()) {
            suffix++;
            candidate = (base.length() > 36 ? base.substring(0, 36) : base) + "_" + suffix;
        }
        return candidate;
    }

    /* ==================== 导出 ==================== */

    public void exportVisits(Long managerId, String month, OutputStream out) {
        List<VisitView> list = visits.findAll(managerId, month);
        if (list.isEmpty()) {
            throw BizException.badRequest("还没有拜访记录，先登记后再导出");
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            Sheet sheet = workbook.createSheet("拜访记录");
            String[] headers = {"序号", "客户经理", "集团编码", "集团名称", "计划月份", "拜访日期", "拜访方式", "拜访结果", "拜访记录"};
            writeHeader(sheet, headers, headerStyle);
            int rowIndex = 1;
            for (VisitView visit : list) {
                Row row = sheet.createRow(rowIndex);
                int col = 0;
                row.createCell(col++).setCellValue(rowIndex);
                row.createCell(col++).setCellValue(visit.managerName());
                row.createCell(col++).setCellValue(visit.groupCode());
                row.createCell(col++).setCellValue(visit.groupName());
                row.createCell(col++).setCellValue(visit.planMonth());
                row.createCell(col++).setCellValue(visit.visitDate() == null ? "" : visit.visitDate().toString());
                row.createCell(col++).setCellValue(visit.method());
                row.createCell(col++).setCellValue(visit.result());
                row.createCell(col).setCellValue(visit.remark() == null ? "" : visit.remark());
                rowIndex++;
            }
            setWidths(sheet, new int[]{6, 10, 12, 26, 10, 12, 12, 12, 40});
            sheet.createFreezePane(0, 1);

            Sheet summary = workbook.createSheet("按集团汇总");
            writeHeader(summary, new String[]{"集团编码", "集团名称", "客户经理", "拜访次数", "计划月份"}, headerStyle);
            Map<String, VisitSummary> grouped = new LinkedHashMap<>();
            for (VisitView visit : list) {
                VisitSummary item = grouped.computeIfAbsent(visit.groupCode(),
                        key -> new VisitSummary(visit.groupCode(), visit.groupName(), visit.managerName()));
                item.count++;
                if (!item.months.contains(visit.planMonth())) {
                    item.months.add(visit.planMonth());
                }
            }
            int summaryRow = 1;
            for (VisitSummary item : grouped.values()) {
                Row row = summary.createRow(summaryRow++);
                row.createCell(0).setCellValue(item.code);
                row.createCell(1).setCellValue(item.name);
                row.createCell(2).setCellValue(item.manager);
                row.createCell(3).setCellValue(item.count);
                row.createCell(4).setCellValue(String.join("、", item.months.stream().sorted().toList()));
            }
            setWidths(summary, new int[]{12, 26, 12, 10, 24});
            workbook.write(out);
        } catch (IOException e) {
            throw new BizException(500, "导出失败：" + e.getMessage());
        }
    }

    public void exportFull(Long managerId, OutputStream out, String month) {
        List<PlanView> list = plans.findAll(managerId, month);
        if (list.isEmpty()) {
            throw BizException.badRequest("暂无可导出的清单数据");
        }
        int total = 0;
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            Map<String, List<PlanView>> byManager = new LinkedHashMap<>();
            for (PlanView plan : list) {
                byManager.computeIfAbsent(plan.managerName(), key -> new ArrayList<>()).add(plan);
            }
            for (Map.Entry<String, List<PlanView>> entry : byManager.entrySet()) {
                List<PlanView> plansOfManager = entry.getValue();
                Set<String> months = new LinkedHashSet<>();
                Map<String, PlanCell> grouped = new LinkedHashMap<>();
                for (PlanView plan : plansOfManager) {
                    months.add(plan.planMonth());
                    PlanCell cell = grouped.computeIfAbsent(plan.groupCode(),
                            key -> new PlanCell(plan.groupCode(), plan.groupName()));
                    cell.months.add(plan.planMonth());
                    cell.name = plan.groupName();
                }
                List<String> monthList = months.stream().sorted().toList();
                Sheet sheet = workbook.createSheet(safeSheetName(entry.getKey(), workbook));
                List<String> headers = new ArrayList<>(List.of("集团编码", "集团名称"));
                headers.addAll(monthList);
                writeHeader(sheet, headers.toArray(new String[0]), headerStyle);
                int rowIndex = 1;
                for (PlanCell cell : grouped.values()) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(cell.code);
                    row.createCell(1).setCellValue(cell.name);
                    for (int m = 0; m < monthList.size(); m++) {
                        if (cell.months.contains(monthList.get(m))) {
                            row.createCell(m + 2).setCellValue("✓");
                        }
                    }
                    total++;
                }
                int[] widths = new int[monthList.size() + 2];
                widths[0] = 12;
                widths[1] = 28;
                for (int m = 2; m < widths.length; m++) {
                    widths[m] = 12;
                }
                setWidths(sheet, widths);
            }
            if (byManager.isEmpty()) {
                Sheet sheet = workbook.createSheet("清单");
                writeHeader(sheet, new String[]{"集团编码", "集团名称"}, headerStyle);
            }

            Sheet detail = workbook.createSheet("拜访明细");
            writeHeader(detail, new String[]{"客户经理", "集团编码", "集团名称", "拜访月份", "状态",
                    "拜访日期", "拜访方式", "拜访结果", "拜访记录"}, headerStyle);
            int detailRow = 1;
            for (PlanView plan : list) {
                Row row = detail.createRow(detailRow++);
                int col = 0;
                row.createCell(col++).setCellValue(plan.managerName());
                row.createCell(col++).setCellValue(plan.groupCode());
                row.createCell(col++).setCellValue(plan.groupName());
                row.createCell(col++).setCellValue(plan.planMonth());
                row.createCell(col++).setCellValue(plan.isVisited() ? "已拜访" : "未拜访");
                row.createCell(col++).setCellValue(plan.visitDate() == null ? "" : plan.visitDate().toString());
                row.createCell(col++).setCellValue(plan.method() == null ? "" : plan.method());
                row.createCell(col++).setCellValue(plan.result() == null ? "" : plan.result());
                row.createCell(col).setCellValue(plan.visitRemark() == null ? "" : plan.visitRemark());
            }
            setWidths(detail, new int[]{10, 12, 24, 12, 10, 12, 12, 12, 34});
            workbook.write(out);
        } catch (IOException e) {
            throw new BizException(500, "导出失败：" + e.getMessage());
        }
    }

    public void template(OutputStream out) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            String m1 = Periods.currentMonth();
            String m2 = YearMonth.parse(m1).plusMonths(1).toString();
            String[] monthHeaders = {m1, m2};
            String[][] samples = {
                    {"小李", "G001", "示例集团（可删除）"},
                    {"小甘", "G002", "示例客户（可删除）"}
            };
            for (String[] sample : samples) {
                Sheet sheet = workbook.createSheet(sample[0]);
                List<String> headers = new ArrayList<>(List.of("集团编码", "集团名称"));
                headers.addAll(List.of(monthHeaders));
                writeHeader(sheet, headers.toArray(new String[0]), headerStyle);
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue(sample[1]);
                row.createCell(1).setCellValue(sample[2]);
                row.createCell(2).setCellValue("是");
                setWidths(sheet, new int[]{12, 28, 12, 12});
            }
            Sheet tips = workbook.createSheet("说明");
            String[] lines = {
                    "填写说明",
                    "1. 每个工作表（Sheet）名称 = 客户经理姓名，例如「小李」",
                    "2. 表头必须包含：集团编码、集团名称，以及月份列（2026-10 或 十月 或 10月）",
                    "3. 月份列填任意内容（如 是 / ✓ / 日期）都表示该月需要拜访",
                    "4. 同一集团可分配给多位客户经理，互不影响",
                    "5. 导入时可选择「合并」（保留原有记录）或「按范围替换」（清空本次涉及的经理+月份后重导）"
            };
            for (int i = 0; i < lines.length; i++) {
                tips.createRow(i).createCell(0).setCellValue(lines[i]);
            }
            tips.setColumnWidth(0, 100 * 256);
            workbook.write(out);
        } catch (IOException e) {
            throw new BizException(500, "模板生成失败：" + e.getMessage());
        }
    }

    public byte[] exportBytes(boolean visitsOnly, Long managerId, String month) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (visitsOnly) {
            exportVisits(managerId, month, buffer);
        } else {
            exportFull(managerId, buffer, month);
        }
        return buffer.toByteArray();
    }

    /* ==================== 工具 ==================== */

    private List<List<String>> readGrid(Sheet sheet, DataFormatter formatter) {
        List<List<String>> grid = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            List<String> values = new ArrayList<>();
            if (row != null) {
                int lastCell = Math.max(row.getLastCellNum(), 0);
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
            }
            grid.add(values);
        }
        return grid;
    }

    private int findHeaderRow(List<List<String>> grid) {
        for (int r = 0; r < Math.min(6, grid.size()); r++) {
            for (String value : grid.get(r)) {
                if (value.contains("集团编码") || value.equalsIgnoreCase("code")
                        || (value.contains("编码") && value.contains("集团"))) {
                    return r;
                }
            }
        }
        return -1;
    }

    private String parseMonthHeader(String header) {
        if (header == null) {
            return null;
        }
        String text = header.replace(" ", "");
        Matcher yearMonth = YEAR_MONTH.matcher(text);
        if (yearMonth.find()) {
            int year = Integer.parseInt(yearMonth.group(1));
            int month = Integer.parseInt(yearMonth.group(2));
            if (month >= 1 && month <= 12) {
                return String.format("%04d-%02d", year, month);
            }
            return null;
        }
        for (Map.Entry<String, Integer> entry : CN_MONTHS.entrySet()) {
            if (text.contains(entry.getKey())) {
                return String.format("%04d-%02d", LocalDate.now().getYear(), entry.getValue());
            }
        }
        Matcher monthOnly = MONTH_ONLY.matcher(text);
        if (monthOnly.find()) {
            int month = Integer.parseInt(monthOnly.group(1));
            if (month >= 1 && month <= 12) {
                return String.format("%04d-%02d", LocalDate.now().getYear(), month);
            }
        }
        return null;
    }

    private static String cell(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',' || ch == '\t' || ch == ';') {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setWidths(Sheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private String safeSheetName(String name, Workbook workbook) {
        String safe = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        if (safe.length() > 31) {
            safe = safe.substring(0, 31);
        }
        String candidate = safe;
        int suffix = 1;
        while (workbook.getSheet(candidate) != null) {
            String tail = "_" + (++suffix);
            candidate = (safe.length() + tail.length() > 31 ? safe.substring(0, 31 - tail.length()) : safe) + tail;
        }
        return candidate;
    }

    private static final class VisitSummary {
        private final String code;
        private final String name;
        private final String manager;
        private int count;
        private final Set<String> months = new LinkedHashSet<>();

        private VisitSummary(String code, String name, String manager) {
            this.code = code;
            this.name = name;
            this.manager = manager;
        }
    }

    private static final class PlanCell {
        private final String code;
        private String name;
        private final Set<String> months = new LinkedHashSet<>();

        private PlanCell(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
