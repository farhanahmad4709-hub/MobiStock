package com.mobistock.dto;


import com.mobistock.model.Sale;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardDTO {
    private long phonesInStock;
    private BigDecimal todaySales;
    private BigDecimal todayProfit;
    private List<Sale> recentSales;
}
