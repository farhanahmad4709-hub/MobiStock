package com.mobistock.controller;

import com.mobistock.service.CsvExportService;
import com.mobistock.service.ExcelExportService;
import com.mobistock.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final CsvExportService csvExportService;
    private final ExcelExportService excelExportService;

    @GetMapping
    public String defaultReport(Model model) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.withDayOfMonth(1);
        populateReportModel(model, from, to);
        return "reports/index";
    }

    @GetMapping("/filter")
    public String filteredReport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.withDayOfMonth(1);
        populateReportModel(model, from, to);
        return "reports/index";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCSV(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.withDayOfMonth(1);

        Map<String, Object> data = reportService.getDateRangeReport(from, to);
        byte[] csvBytes = csvExportService.generateReportCsv(data, from, to);

        String filename = String.format("financial_report_%s_to_%s.csv", from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws java.io.IOException {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.withDayOfMonth(1);

        Map<String, Object> data = reportService.getDateRangeReport(from, to);
        byte[] excelBytes = excelExportService.generateReportExcel(data, from, to);

        String filename = String.format("financial_report_%s_to_%s.xlsx", from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    private void populateReportModel(Model model, LocalDate from, LocalDate to) {
        Map<String, Object> data = reportService.getDateRangeReport(from, to);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("totalSales", data.getOrDefault("totalSales", 0));
        model.addAttribute("totalProfit", data.getOrDefault("totalProfit", 0));
        model.addAttribute("salesCount", data.getOrDefault("salesCount", 0));
        model.addAttribute("sales", data.getOrDefault("salesList", null));
        model.addAttribute("salesByDay", data.getOrDefault("salesByDay", null));
        model.addAttribute("topSellingItems", data.getOrDefault("topSellingItems", null));
        model.addAttribute("pageTitle", "Reports & Analytics");
    }
}
