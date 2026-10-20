package com.mobistock.service;

import com.mobistock.model.Sale;
import com.mobistock.model.SaleItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] generateSalesHistoryExcel(List<Sale> sales, LocalDate fromDate, LocalDate toDate, String query) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales History");
            sheet.setDisplayGridlines(true);

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle metaStyle = createMetaStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook, new byte[]{31, 78, 121}); // Deep Blue
            CellStyle textStyle = createTextStyle(workbook, false);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook, false);
            CellStyle totalCurrencyStyle = createCurrencyStyle(workbook, true);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);

            int rowIdx = 0;

            // Title
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("MobiStock - Sales History Report");
            titleCell.setCellStyle(titleStyle);

            // Metadata
            Row meta1 = sheet.createRow(rowIdx++);
            meta1.createCell(0).setCellValue("Date Range: " + fromDate + " to " + toDate);
            meta1.getCell(0).setCellStyle(metaStyle);

            Row meta2 = sheet.createRow(rowIdx++);
            meta2.createCell(0).setCellValue("Search Query: " + ((query != null && !query.trim().isEmpty()) ? query.trim() : "All Records"));
            meta2.getCell(0).setCellStyle(metaStyle);

            Row meta3 = sheet.createRow(rowIdx++);
            meta3.createCell(0).setCellValue("Total Transactions: " + sales.size());
            meta3.getCell(0).setCellStyle(metaStyle);

            rowIdx++; // Blank row

            // Table Header
            String[] headers = {
                    "Invoice #", "Date", "Time", "Customer Name", "Customer Phone",
                    "Sold By", "Items Count", "Items Summary", "Total Amount (PKR)", "Total Profit (PKR)", "Notes"
            };

            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;

            // Data Rows
            for (Sale sale : sales) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                // Invoice #
                Cell c0 = row.createCell(0);
                c0.setCellValue(sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "");
                c0.setCellStyle(centerStyle);

                // Date
                Cell c1 = row.createCell(1);
                c1.setCellValue(sale.getCreatedAt() != null ? sale.getCreatedAt().format(DATE_FMT) : "");
                c1.setCellStyle(centerStyle);

                // Time
                Cell c2 = row.createCell(2);
                c2.setCellValue(sale.getCreatedAt() != null ? sale.getCreatedAt().format(TIME_FMT) : "");
                c2.setCellStyle(centerStyle);

                // Customer Name
                Cell c3 = row.createCell(3);
                String custName = (sale.getCustomerName() != null && !sale.getCustomerName().trim().isEmpty())
                        ? sale.getCustomerName().trim() : "NaN";
                c3.setCellValue(custName);
                c3.setCellStyle(textStyle);

                // Customer Phone (explicitly text so Excel doesn't turn it into scientific notation)
                Cell c4 = row.createCell(4);
                String phone = formatPhone(sale.getCustomerPhone());
                c4.setCellValue(phone);
                c4.setCellStyle(centerStyle);

                // Sold By
                Cell c5 = row.createCell(5);
                String soldBy = (sale.getSoldBy() != null && sale.getSoldBy().getFullName() != null)
                        ? sale.getSoldBy().getFullName() : "System";
                c5.setCellValue(soldBy);
                c5.setCellStyle(textStyle);

                // Items Count
                Cell c6 = row.createCell(6);
                int itemsCount = sale.getItems() != null ? sale.getItems().size() : 0;
                c6.setCellValue(itemsCount);
                c6.setCellStyle(centerStyle);

                // Items Summary
                Cell c7 = row.createCell(7);
                StringBuilder itemsSummary = new StringBuilder();
                if (sale.getItems() != null) {
                    for (int i = 0; i < sale.getItems().size(); i++) {
                        SaleItem item = sale.getItems().get(i);
                        if (i > 0) itemsSummary.append("; ");
                        itemsSummary.append(item.getItemName());
                        if (item.getPhone() != null && item.getPhone().getImei() != null && !item.getPhone().getImei().isEmpty()) {
                            itemsSummary.append(" [IMEI: ").append(item.getPhone().getImei()).append("]");
                        }
                        itemsSummary.append(" x").append(item.getQuantity() != null ? item.getQuantity() : 1);
                        if (item.getUnitPrice() != null) {
                            itemsSummary.append(" @ PKR ").append(String.format(Locale.US, "%.2f", item.getUnitPrice()));
                        }
                    }
                }
                c7.setCellValue(itemsSummary.toString());
                c7.setCellStyle(textStyle);

                // Total Amount
                Cell c8 = row.createCell(8);
                BigDecimal amt = sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO;
                c8.setCellValue(amt.doubleValue());
                c8.setCellStyle(currencyStyle);
                totalRevenue = totalRevenue.add(amt);

                // Total Profit
                Cell c9 = row.createCell(9);
                BigDecimal profit = sale.getTotalProfit() != null ? sale.getTotalProfit() : BigDecimal.ZERO;
                c9.setCellValue(profit.doubleValue());
                c9.setCellStyle(currencyStyle);
                totalProfit = totalProfit.add(profit);

                // Notes
                Cell c10 = row.createCell(10);
                c10.setCellValue(sale.getNotes() != null ? sale.getNotes() : "");
                c10.setCellStyle(textStyle);
            }

            // Summary Totals Row
            Row totalRow = sheet.createRow(rowIdx++);
            totalRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = totalRow.createCell(i);
                cell.setCellStyle(totalLabelStyle);
            }
            totalRow.getCell(0).setCellValue("TOTAL SUMMARY");
            totalRow.getCell(6).setCellValue(sales.size() + " orders");
            totalRow.getCell(6).setCellStyle(totalLabelStyle);

            Cell totAmtCell = totalRow.getCell(8);
            totAmtCell.setCellValue(totalRevenue.doubleValue());
            totAmtCell.setCellStyle(totalCurrencyStyle);

            Cell totProfCell = totalRow.getCell(9);
            totProfCell.setCellValue(totalProfit.doubleValue());
            totProfCell.setCellStyle(totalCurrencyStyle);

            // Auto-size columns with minimum widths
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 3800) {
                    sheet.setColumnWidth(i, 3800);
                } else {
                    sheet.setColumnWidth(i, currentWidth + 800); // safety padding
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    public byte[] generateReportExcel(Map<String, Object> reportData, LocalDate fromDate, LocalDate toDate) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle metaStyle = createMetaStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook, new byte[]{13, 110, (byte) 253}); // Bootstrap Primary
            CellStyle subHeaderStyle = createHeaderStyle(workbook, new byte[]{40, (byte) 167, 69}); // Success Green
            CellStyle textStyle = createTextStyle(workbook, false);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook, false);
            CellStyle totalCurrencyStyle = createCurrencyStyle(workbook, true);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);

            // ==================== SHEET 1: Financial Overview & Daily Sales ====================
            Sheet sheet1 = workbook.createSheet("Executive Overview");
            sheet1.setDisplayGridlines(true);

            int r = 0;
            Row tRow = sheet1.createRow(r++);
            Cell tCell = tRow.createCell(0);
            tCell.setCellValue("MobiStock - Financial & Business Performance Report");
            tCell.setCellStyle(titleStyle);

            Row mRow = sheet1.createRow(r++);
            mRow.createCell(0).setCellValue("Reporting Period: " + fromDate + " to " + toDate);
            mRow.getCell(0).setCellStyle(metaStyle);

            r++; // blank

            // Executive Summary Table
            Row esHeader = sheet1.createRow(r++);
            esHeader.createCell(0).setCellValue("Executive Summary Metric");
            esHeader.getCell(0).setCellStyle(headerStyle);
            esHeader.createCell(1).setCellValue("Value");
            esHeader.getCell(1).setCellStyle(headerStyle);

            BigDecimal totalSales = (BigDecimal) reportData.getOrDefault("totalSales", BigDecimal.ZERO);
            BigDecimal totalProfit = (BigDecimal) reportData.getOrDefault("totalProfit", BigDecimal.ZERO);
            Object salesCount = reportData.getOrDefault("salesCount", 0L);

            Row rRev = sheet1.createRow(r++);
            rRev.createCell(0).setCellValue("Total Revenue (PKR)");
            rRev.getCell(0).setCellStyle(textStyle);
            rRev.createCell(1).setCellValue(totalSales.doubleValue());
            rRev.getCell(1).setCellStyle(currencyStyle);

            Row rProf = sheet1.createRow(r++);
            rProf.createCell(0).setCellValue("Total Net Profit (PKR)");
            rProf.getCell(0).setCellStyle(textStyle);
            rProf.createCell(1).setCellValue(totalProfit.doubleValue());
            rProf.getCell(1).setCellStyle(currencyStyle);

            Row rCount = sheet1.createRow(r++);
            rCount.createCell(0).setCellValue("Total Completed Transactions");
            rCount.getCell(0).setCellStyle(textStyle);
            rCount.createCell(1).setCellValue(String.valueOf(salesCount));
            rCount.getCell(1).setCellStyle(centerStyle);

            r += 2; // blank rows

            // Daily Sales Breakdown
            Row dsTitle = sheet1.createRow(r++);
            Cell dsCell = dsTitle.createCell(0);
            dsCell.setCellValue("Daily Sales Breakdown");
            dsCell.setCellStyle(titleStyle);

            Row dsHeader = sheet1.createRow(r++);
            dsHeader.createCell(0).setCellValue("Date");
            dsHeader.getCell(0).setCellStyle(subHeaderStyle);
            dsHeader.createCell(1).setCellValue("Sales Amount (PKR)");
            dsHeader.getCell(1).setCellStyle(subHeaderStyle);

            Object salesByDayObj = reportData.get("salesByDay");
            double dailySum = 0;
            if (salesByDayObj instanceof List<?>) {
                List<Map<String, Object>> dailyList = (List<Map<String, Object>>) salesByDayObj;
                for (Map<String, Object> day : dailyList) {
                    Row dRow = sheet1.createRow(r++);
                    Cell d0 = dRow.createCell(0);
                    d0.setCellValue(String.valueOf(day.getOrDefault("date", "")));
                    d0.setCellStyle(centerStyle);

                    Cell d1 = dRow.createCell(1);
                    Object sVal = day.get("sales");
                    double sDbl = (sVal instanceof BigDecimal) ? ((BigDecimal) sVal).doubleValue() : 0.0;
                    dailySum += sDbl;
                    d1.setCellValue(sDbl);
                    d1.setCellStyle(currencyStyle);
                }
            }

            // Daily Totals Row
            Row dsTotal = sheet1.createRow(r++);
            dsTotal.createCell(0).setCellValue("Total Period Sales");
            dsTotal.getCell(0).setCellStyle(totalLabelStyle);
            Cell dsTotalAmt = dsTotal.createCell(1);
            dsTotalAmt.setCellValue(dailySum);
            dsTotalAmt.setCellStyle(totalCurrencyStyle);

            sheet1.setColumnWidth(0, 9000);
            sheet1.setColumnWidth(1, 9000);

            // ==================== SHEET 2: Top Selling Products ====================
            Sheet sheet2 = workbook.createSheet("Top Selling Products");
            sheet2.setDisplayGridlines(true);

            int r2 = 0;
            Row pTitle = sheet2.createRow(r2++);
            Cell ptCell = pTitle.createCell(0);
            ptCell.setCellValue("Top Selling Products / Models");
            ptCell.setCellStyle(titleStyle);

            Row pMeta = sheet2.createRow(r2++);
            pMeta.createCell(0).setCellValue("Period: " + fromDate + " to " + toDate);
            pMeta.getCell(0).setCellStyle(metaStyle);

            r2++;

            Row pHeader = sheet2.createRow(r2++);
            pHeader.createCell(0).setCellValue("Rank");
            pHeader.getCell(0).setCellStyle(headerStyle);
            pHeader.createCell(1).setCellValue("Product / Model Name");
            pHeader.getCell(1).setCellStyle(headerStyle);
            pHeader.createCell(2).setCellValue("Units Sold");
            pHeader.getCell(2).setCellStyle(headerStyle);

            Object topItemsObj = reportData.get("topSellingItems");
            if (topItemsObj instanceof List<?>) {
                List<Map<String, Object>> topList = (List<Map<String, Object>>) topItemsObj;
                int rank = 1;
                for (Map<String, Object> item : topList) {
                    Row pr = sheet2.createRow(r2++);
                    Cell cRank = pr.createCell(0);
                    cRank.setCellValue(rank++);
                    cRank.setCellStyle(centerStyle);

                    Cell cName = pr.createCell(1);
                    cName.setCellValue(String.valueOf(item.getOrDefault("name", "")));
                    cName.setCellStyle(textStyle);

                    Cell cQty = pr.createCell(2);
                    Object q = item.getOrDefault("quantity", 0);
                    cQty.setCellValue(q instanceof Number ? ((Number) q).intValue() : 0);
                    cQty.setCellStyle(centerStyle);
                }
            }

            sheet2.setColumnWidth(0, 3000);
            sheet2.setColumnWidth(1, 14000);
            sheet2.setColumnWidth(2, 5000);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public static String formatPhone(String phone) {
        if (phone == null || phone.trim().isEmpty() || phone.equalsIgnoreCase("nan") || phone.equalsIgnoreCase("null")) {
            return "NaN";
        }
        String clean = phone.replaceAll("[^0-9+]", "");
        if (clean.length() == 11 && clean.startsWith("0")) {
            return clean.substring(0, 4) + "-" + clean.substring(4);
        }
        if (clean.length() == 10) {
            return "0" + clean.substring(0, 3) + "-" + clean.substring(3);
        }
        if (clean.length() == 12 && clean.startsWith("92")) {
            return "+92-" + clean.substring(2, 5) + "-" + clean.substring(5);
        }
        if (clean.length() > 8 && !clean.contains("-")) {
            int mid = clean.length() / 2;
            return clean.substring(0, mid) + "-" + clean.substring(mid);
        }
        return phone.trim();
    }

    // --- Styling Helpers ---

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createMetaStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        XSSFColor color = new XSSFColor(rgb, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createTextStyle(Workbook wb, boolean bold) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setBold(bold);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb, boolean bold) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setBold(bold);
        style.setFont(font);

        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createTotalLabelStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private void setBorders(CellStyle style, BorderStyle b) {
        style.setBorderTop(b);
        style.setBorderBottom(b);
        style.setBorderLeft(b);
        style.setBorderRight(b);
    }
}
