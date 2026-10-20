package com.mobistock.controller;


import com.mobistock.service.PhoneService;
import com.mobistock.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final PhoneService phoneService;
    private final SaleService saleService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("phonesInStock", phoneService.countInStock());
        model.addAttribute("todaySales", saleService.getTodaySalesTotal());
        model.addAttribute("todayProfit", saleService.getTodayProfitTotal());
        model.addAttribute("recentSales", saleService.getRecentSales(100));
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard";
    }
}
