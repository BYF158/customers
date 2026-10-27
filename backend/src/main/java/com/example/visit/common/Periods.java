package com.example.visit.common;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 月份（YYYY-MM）相关的校验与换算。
 */
public final class Periods {

    private static final Pattern MONTH = Pattern.compile("^(\\d{4})-(0[1-9]|1[0-2])$");
    private static final Pattern LOOSE = Pattern.compile("^(\\d{4})\\s*[-/.年]\\s*(\\d{1,2})");

    private Periods() {
    }

    public static String currentMonth() {
        return YearMonth.now().toString();
    }

    /**
     * 严格校验并规范化月份，非法时抛 400。
     */
    public static String requireMonth(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw BizException.badRequest("月份格式不正确，应为 YYYY-MM，例如 2026-10");
        }
        return normalized;
    }

    /**
     * 宽松解析：支持 2026-10 / 2026/10 / 2026年10月 / 2026-10-15，返回 YYYY-MM；无法解析返回 null。
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        Matcher exact = MONTH.matcher(text);
        if (exact.matches()) {
            return text;
        }
        Matcher loose = LOOSE.matcher(text);
        if (loose.find()) {
            int year = Integer.parseInt(loose.group(1));
            int month = Integer.parseInt(loose.group(2));
            if (month >= 1 && month <= 12) {
                return String.format("%04d-%02d", year, month);
            }
        }
        return null;
    }

    /**
     * 计划月份的最后一天（用于"计划月内完成"判断）。
     */
    public static LocalDate lastDay(String month) {
        return YearMonth.parse(month).atEndOfMonth();
    }

    public static int yearOf(String month) {
        return Integer.parseInt(month.substring(0, 4));
    }
}
