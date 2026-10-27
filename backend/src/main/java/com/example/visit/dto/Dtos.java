package com.example.visit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 请求体集合（Jackson 反序列化 + Bean Validation 均支持 record）。
 */
public final class Dtos {

    private Dtos() {
    }

    public record LoginReq(
            @NotBlank(message = "请输入账号") String username,
            @NotBlank(message = "请输入密码") String password) {
    }

    public record ChangePasswordReq(
            @NotBlank(message = "请输入当前密码") String oldPassword,
            @NotBlank(message = "请输入新密码") @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位") String newPassword) {
    }

    public record CreateUserReq(
            @NotBlank(message = "请输入账号") @Size(max = 50, message = "账号过长") String username,
            @NotBlank(message = "请输入姓名") @Size(max = 50, message = "姓名过长") String realName,
            @NotBlank(message = "请输入初始密码") @Size(min = 6, max = 32, message = "密码长度需为 6-32 位") String password,
            @NotBlank(message = "请选择角色") String role) {
    }

    public record UpdateUserReq(
            @NotBlank(message = "请输入姓名") @Size(max = 50, message = "姓名过长") String realName,
            @NotNull(message = "请选择状态") Boolean enabled) {
    }

    public record ResetPasswordReq(
            @NotBlank(message = "请输入新密码") @Size(min = 6, max = 32, message = "密码长度需为 6-32 位") String password) {
    }

    public record GroupReq(
            @NotBlank(message = "请输入集团编码") @Size(max = 30, message = "编码过长") String code,
            @NotBlank(message = "请输入集团名称") @Size(max = 120, message = "名称过长") String name) {
    }

    public record PlanCreateReq(
            @NotNull(message = "请选择客户经理") Long managerId,
            @NotNull(message = "请选择集团") Long groupId,
            @NotBlank(message = "请选择拜访月份") String planMonth,
            @Size(max = 255, message = "备注过长") String remark) {
    }

    public record PlanUpdateReq(
            Long managerId,
            Long groupId,
            String planMonth,
            @Size(max = 255, message = "备注过长") String remark) {
    }

    public record VisitCreateReq(
            @NotNull(message = "请选择计划") Long planId,
            @NotNull(message = "请选择拜访日期") LocalDate visitDate,
            @NotBlank(message = "请选择拜访方式") String method,
            @NotBlank(message = "请选择拜访结果") String result,
            @Size(max = 1000, message = "拜访记录最多 1000 字") String remark) {
    }

    public record VisitUpdateReq(
            LocalDate visitDate,
            String method,
            String result,
            @Size(max = 1000, message = "拜访记录最多 1000 字") String remark) {
    }

    public record ImportRow(
            String managerName,
            String code,
            String name,
            String planMonth) {
    }

    public record ImportApplyReq(
            String mode,
            List<ImportRow> rows) {
    }
}
