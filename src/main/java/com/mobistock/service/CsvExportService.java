package com.mobistock.service;

import com.mobistock.model.Sale;
import com.mobistock.model.SaleItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CsvExportService {

    private static final String UTF8_BOM = "\uFEFF";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Generates a strict RFC 4180 compliant CSV for Sales History where Row 1 is the table header,
     * ensuring 0 parse warnings and perfect multi-column parsing in online CSV viewers and Excel.
     */
    public byte[] generateSalesHistoryCsv(List<Sale> sales, LocalDate fromDate, LocalDate toDate, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);

        // Row 1: Strict Table Column Headers (11 columns)
        sb.append(toCsvLine(
                "Invoice #",
                "Date",
                "Time",
                "Customer Name",
                "Customer Phone",
                "Sold By",
                "Items Count",
                "Items Summary",
                "Total Amount (PKR)",
                "Total Profit (PKR)",
                "Notes"
        ));

        BigDecimal totalSalesRevenue = BigDecimal.ZERO;
        BigDecimal totalSalesProfit = BigDecimal.ZERO;

        // Data Rows (Exactly 11 columns on every row)
        for (Sale sale : sales) {
            String invoice = sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "";
            String date = sale.getCreatedAt() != null ? sale.getCreatedAt().format(DATE_FORMATTER) : "";
            String time = sale.getCreatedAt() != null ? sale.getCreatedAt().format(TIME_FORMATTER) : "";
            String customerName = (sale.getCustomerName() != null && !sale.getCustomerName().trim().isEmpty())
                    ? sale.getCustomerName().trim() : "NaN";
            
            // Format phone to prevent scientific notation in Excel (e.g. 0349-0000000)
            String customerPhone = ExcelExportService.formatPhone(sale.getCustomerPhone());

            String soldBy = (sale.getSoldBy() != null && sale.getSoldBy().getFullName() != null)
                    ? sale.getSoldBy().getFullName() : "System";

            List<SaleItem> items = sale.getItems();
            int itemsCount = items != null ? items.size() : 0;

            StringBuilder itemSummaryBuilder = new StringBuilder();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    SaleItem item = items.get(i);
                    if (i > 0) itemSummaryBuilder.append("; ");
                    itemSummaryBuilder.append(item.getItemName());
                    if (item.getPhone() != null && item.getPhone().getImei() != null && !item.getPhone().getImei().isEmpty()) {
                        itemSummaryBuilder.append(" [IMEI: ").append(item.getPhone().getImei()).append("]");
                    }
                    itemSummaryBuilder.append(" x").append(item.getQuantity() != null ? item.getQuantity() : 1);
                    if (item.getUnitPrice() != null) {
                        itemSummaryBuilder.append(" @ PKR ").append(String.format(Locale.US, "%.2f", item.getUnitPrice()));
                    }
                }
            }

            BigDecimal totalAmount = sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal totalProfit = sale.getTotalProfit() != null ? sale.getTotalProfit() : BigDecimal.ZERO;

            totalSalesRevenue = totalSalesRevenue.add(totalAmount);
            totalSalesProfit = totalSalesProfit.add(totalProfit);

            String notes = sale.getNotes() != null ? sale.getNotes() : "";

            sb.append(toCsvLine(
                    invoice,
                    date,
                    time,
                    customerName,
                    customerPhone,
                    soldBy,
                    itemsCount,
                    itemSummaryBuilder.toString(),
                    String.format(Locale.US, "%.2f", totalAmount),
                    String.format(Locale.US, "%.2f", totalProfit),
                    notes
            ));
        }

        // Summary Totals Row (Exactly 11 columns so table dimensions remain uniform)
        sb.append(toCsvLine(
                "TOTAL SUMMARY",
                fromDate + " to " + toDate,
                "",
                "",
                "",
                "",
                sales.size() + " orders",
                "",
                String.format(Locale.US, "%.2f", totalSalesRevenue),
                String.format(Locale.US, "%.2f", totalSalesProfit),
                ""
        ));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a strict uniform 4-column CSV for Financial Performance Reports.
     * Starts with headers on Row 1 with identical column counts on all rows for 0 parse warnings.
     */
    @SuppressWarnings("unchecked")
    public byte[] generateReportCsv(Map<String, Object> reportData, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);

        // Row 1: Strict Table Column Headers (4 columns)
        sb.append(toCsvLine("Section", "Item / Date / Description", "Quantity / Count", "Amount (PKR)"));

        // Section 1: Executive Financial Summary
        BigDecimal totalSales = (BigDecimal) reportData.getOrDefault("totalSales", BigDecimal.ZERO);
        BigDecimal totalProfit = (BigDecimal) reportData.getOrDefault("totalProfit", BigDecimal.ZERO);
        Object salesCountObj = reportData.getOrDefault("salesCount", 0L);

        sb.append(toCsvLine("Executive Summary", "Total Revenue (" + fromDate + " to " + toDate + ")", "-", String.format(Locale.US, "%.2f", totalSales)));
        sb.append(toCsvLine("Executive Summary", "Total Net Profit", "-", String.format(Locale.US, "%.2f", totalProfit)));
        sb.append(toCsvLine("Executive Summary", "Total Completed Transactions", String.valueOf(salesCountObj), "-"));

        // Section 2: Daily Sales Breakdown
        Object salesByDayObj = reportData.get("salesByDay");
        if (salesByDayObj instanceof List<?>) {
            List<Map<String, Object>> salesByDay = (List<Map<String, Object>>) salesByDayObj;
            for (Map<String, Object> dayEntry : salesByDay) {
                String date = String.valueOf(dayEntry.getOrDefault("date", ""));
                Object salesVal = dayEntry.get("sales");
                String formattedSales = "0.00";
                if (salesVal instanceof BigDecimal) {
                    formattedSales = String.format(Locale.US, "%.2f", (BigDecimal) salesVal);
                } else if (salesVal != null) {
                    formattedSales = salesVal.toString();
                }
                sb.append(toCsvLine("Daily Sales Breakdown", date, "-", formattedSales));
            }
        }

        // Section 3: Top Selling Products
        Object topSellingItemsObj = reportData.get("topSellingItems");
        if (topSellingItemsObj instanceof List<?>) {
            List<Map<String, Object>> topItems = (List<Map<String, Object>>) topSellingItemsObj;
            int rank = 1;
            for (Map<String, Object> itemEntry : topItems) {
                String name = String.valueOf(itemEntry.getOrDefault("name", ""));
                Object qty = itemEntry.getOrDefault("quantity", 0);
                sb.append(toCsvLine("Top Selling Products", "#" + rank++ + " " + name, String.valueOf(qty), "-"));
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toCsvLine(Object... values) {
        if (values == null || values.length == 0) {
            return "\r\n";
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) line.append(",");
            line.append(escapeCsv(values[i]));
        }
        line.append("\r\n");
        return line.toString();
    }

    private String escapeCsv(Object val) {
        if (val == null) {
            return "\"\"";
        }
        String str = val.toString();
        String escaped = str.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
