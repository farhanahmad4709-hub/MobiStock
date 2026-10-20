package com.mobistock.service;

import com.mobistock.model.Sale;
import com.mobistock.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;

    public Map<String, Object> getDailySales(LocalDate date) {
        return new HashMap<>(); // Placeholder
    }

    public Map<String, Object> getMonthlySales(int year, int month) {
        return new HashMap<>(); // Placeholder
    }

    public Map<String, Object> getDateRangeReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusNanos(1);
        
        Map<String, Object> data = new HashMap<>();
        BigDecimal totalSales = saleRepository.sumTotalAmountBetweenDates(start, end);
        BigDecimal totalProfit = saleRepository.sumTotalProfitBetweenDates(start, end);
        long salesCount = saleRepository.countByCreatedAtBetween(start, end);
        
        data.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        data.put("totalProfit", totalProfit != null ? totalProfit : BigDecimal.ZERO);
        data.put("salesCount", salesCount);
        
        // Sales By Day
        List<Sale> sales = saleRepository.findByCreatedAtBetween(start, end);
        Map<LocalDate, BigDecimal> dailyTotals = new HashMap<>();
        Map<String, Integer> itemQuantities = new HashMap<>();

        for (Sale sale : sales) {
            LocalDate d = sale.getCreatedAt().toLocalDate();
            dailyTotals.put(d, dailyTotals.getOrDefault(d, BigDecimal.ZERO).add(sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO));
            
            for (com.mobistock.model.SaleItem item : sale.getItems()) {
                String name = item.getItemName();
                if (name != null && !name.isEmpty()) {
                    itemQuantities.put(name, itemQuantities.getOrDefault(name, 0) + item.getQuantity());
                }
            }
        }
        
        List<Map<String, Object>> salesByDay = dailyTotals.entrySet().stream()
            .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("date", e.getKey().toString());
                map.put("sales", e.getValue());
                return map;
            })
            .toList();
            
        List<Map<String, Object>> topSellingItems = itemQuantities.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(10)
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", e.getKey());
                map.put("quantity", e.getValue());
                return map;
            })
            .toList();
            
        data.put("salesByDay", salesByDay);
        data.put("topSellingItems", topSellingItems);
        return data;
    }

    public List<Object> getTopSellingPhones(LocalDate from, LocalDate to, int limit) {
        return Collections.emptyList(); // Placeholder
    }

    public List<Object> getTopSellingAccessories(LocalDate from, LocalDate to, int limit) {
        return Collections.emptyList(); // Placeholder
    }
}
