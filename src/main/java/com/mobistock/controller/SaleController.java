package com.mobistock.controller;

import com.mobistock.dto.SaleDTO;
import com.mobistock.model.Sale;

import com.mobistock.service.CsvExportService;
import com.mobistock.service.ExcelExportService;
import com.mobistock.service.PhoneService;
import com.mobistock.service.SaleService;
import com.mobistock.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService saleService;
    private final UserService userService;
    private final PhoneService phoneService;
    private final CsvExportService csvExportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/new")
    public String newSaleView(Model model) {
        model.addAttribute("pageTitle", "New Sale");
        return "sales/new-sale";
    }

    @PostMapping("/complete")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> completeSale(@RequestBody SaleDTO saleDTO) {
        try {
            Sale sale = saleService.completeSale(saleDTO, userService.getCurrentUser());
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", true, "saleId", sale.getId()));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    public String salesHistory(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q,
            Model model) {
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();
        java.util.List<Sale> sales = saleService.getSalesByDateRange(fromDate, toDate, q);
        java.math.BigDecimal totalSales = sales.stream().map(Sale::getTotalAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalProfit = sales.stream().map(Sale::getTotalProfit).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("sales", sales);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("searchQuery", q);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("pageTitle", "Sales History");
        return "sales/history";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSalesHistory(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q) {
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();

        java.util.List<Sale> sales = saleService.getSalesByDateRange(fromDate, toDate, q);
        byte[] csvBytes = csvExportService.generateSalesHistoryCsv(sales, fromDate, toDate, q);

        String filename = String.format("sales_history_%s_to_%s.csv", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportSalesHistoryExcel(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "q", required = false) String q) throws java.io.IOException {
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();

        java.util.List<Sale> sales = saleService.getSalesByDateRange(fromDate, toDate, q);
        byte[] excelBytes = excelExportService.generateSalesHistoryExcel(sales, fromDate, toDate, q);

        String filename = String.format("sales_history_%s_to_%s.xlsx", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @PostMapping("/delete/{id}")
    public String deleteSale(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            saleService.deleteSale(id);
            redirectAttributes.addFlashAttribute("success", "Transaction cleared and items returned to stock.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error clearing transaction: " + e.getMessage());
        }
        return "redirect:/sales";
    }

    @PostMapping("/return-item/{saleId}/{itemId}")
    public String returnItem(@PathVariable Long saleId, @PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            saleService.returnItem(saleId, itemId);
            redirectAttributes.addFlashAttribute("success", "Item returned to stock successfully.");
            try {
                saleService.getSaleById(saleId);
                return "redirect:/sales/" + saleId;
            } catch (Exception ex) {
                return "redirect:/sales"; // Sale was fully deleted
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error returning item: " + e.getMessage());
            return "redirect:/sales/" + saleId;
        }
    }

    @GetMapping("/{id}")
    public String saleDetail(@PathVariable Long id, Model model) {
        model.addAttribute("sale", saleService.getSaleById(id));
        model.addAttribute("pageTitle", "Sale Details");
        return "sales/detail";
    }

    @GetMapping("/api/lookup/{code}")
    @ResponseBody
    public Object lookupItem(@PathVariable String code) {
        return phoneService.getPhoneByImei(code)
                .map(p -> (Object) p)
                .orElse(null);
    }
}
