package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.dto.Dtos.ImportApplyReq;
import com.example.visit.dto.Views.ImportPreview;
import com.example.visit.dto.Views.ImportResult;
import com.example.visit.security.AdminOnly;
import com.example.visit.security.CurrentUser;
import com.example.visit.security.UserContext;
import com.example.visit.service.ExcelService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private final ExcelService excelService;

    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }

    @AdminOnly
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        excelService.template(buffer);
        return download(buffer.toByteArray(), "拜访计划导入模板.xlsx");
    }

    @AdminOnly
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportPreview> preview(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(excelService.preview(file));
    }

    @AdminOnly
    @PostMapping("/import/apply")
    public ApiResponse<ImportResult> apply(@RequestBody ImportApplyReq req) {
        return ApiResponse.ok(excelService.apply(req));
    }

    @GetMapping("/export/visits")
    public ResponseEntity<byte[]> exportVisits(@RequestParam(required = false) Long managerId,
                                               @RequestParam(required = false) String month) {
        CurrentUser current = UserContext.get();
        Long scope = current.isAdmin() ? managerId : current.id();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        excelService.exportVisits(scope, month, buffer);
        return download(buffer.toByteArray(), "拜访记录_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/export/full")
    public ResponseEntity<byte[]> exportFull(@RequestParam(required = false) Long managerId,
                                             @RequestParam(required = false) String month) {
        CurrentUser current = UserContext.get();
        Long scope = current.isAdmin() ? managerId : current.id();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        excelService.exportFull(scope, buffer, month);
        return download(buffer.toByteArray(), "拜访清单_" + LocalDate.now() + ".xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] content, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(content.length)
                .body(content);
    }
}
