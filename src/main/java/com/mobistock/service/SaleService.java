package com.mobistock.service;

import com.mobistock.dto.SaleDTO;
import com.mobistock.dto.SaleItemDTO;
import com.mobistock.model.*;
import com.mobistock.model.enums.ItemType;
import com.mobistock.model.enums.PhoneStatus;
import com.mobistock.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final PhoneService phoneService;

    @Transactional
    public Sale completeSale(SaleDTO dto, User soldBy) {
        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());
        sale.setSoldBy(soldBy);
        String custName = dto.getCustomerName();
        if (custName == null || custName.trim().isEmpty() || "NaN".equalsIgnoreCase(custName.trim())) {
            custName = "Walk-in Customer";
        }
        sale.setCustomerName(custName.trim());

        String custPhone = dto.getCustomerPhone();
        if (custPhone == null || custPhone.trim().isEmpty() || "NaN".equalsIgnoreCase(custPhone.trim())) {
            custPhone = "-";
        }
        sale.setCustomerPhone(custPhone.trim());
        String notes = dto.getNotes();
        if (notes == null || notes.trim().isEmpty()) {
            notes = "Thank you for shopping with us! We appreciate your business.";
        }
        sale.setNotes(notes.trim());
        sale.setCreatedAt(LocalDateTime.now());
        
        List<SaleItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        if (dto.getItems() != null) {
            for (SaleItemDTO itemDto : dto.getItems()) {
                SaleItem item = new SaleItem();
                item.setSale(sale);
                item.setItemType(ItemType.valueOf(itemDto.getItemType()));
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                
                BigDecimal itemAmount = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                totalAmount = totalAmount.add(itemAmount);
                BigDecimal costPrice;

                if (item.getItemType() == ItemType.PHONE) {
                    Phone phone = phoneService.getPhoneById(itemDto.getPhoneId());
                    if (phone.getStatus() != PhoneStatus.IN_STOCK && phone.getStatus() != PhoneStatus.RETURNED) {
                        throw new RuntimeException("Phone is not available for sale");
                    }
                    phone.setStatus(PhoneStatus.SOLD);
                    phone.setSoldAt(LocalDateTime.now());
                    item.setPhone(phone);
                    costPrice = phone.getCostPrice();
                } else {
                    throw new RuntimeException("Invalid item type");
                }
                
                item.setCostPrice(costPrice);
                BigDecimal profit = itemDto.getUnitPrice().subtract(costPrice).multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                item.setProfit(profit);
                totalProfit = totalProfit.add(profit);
                
                items.add(item);
            }
        }
        
        sale.setItems(items);
        sale.setTotalAmount(totalAmount);
        sale.setTotalProfit(totalProfit);
        
        return saleRepository.save(sale);
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    public List<Sale> getSalesByDateRange(LocalDate from, LocalDate to, String q) {
        List<Sale> sales = saleRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusNanos(1));
        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim().toLowerCase();
            String queryClean = query.replaceAll("[^a-z0-9]", "");
            String[] tokens = query.split("[\\s,;/-]+");

            return sales.stream().filter(sale -> {
                StringBuilder sb = new StringBuilder();
                if (sale.getInvoiceNumber() != null) sb.append(sale.getInvoiceNumber()).append(" ");
                if (sale.getCustomerName() != null) sb.append(sale.getCustomerName()).append(" ");
                if (sale.getSoldBy() != null && sale.getSoldBy().getFullName() != null) sb.append(sale.getSoldBy().getFullName()).append(" ");
                for (SaleItem item : sale.getItems()) {
                    if (item.getItemName() != null) sb.append(item.getItemName()).append(" ");
                    if (item.getPhone() != null) {
                        sb.append(item.getPhone().getImei()).append(" ");
                        if (item.getPhone().getBrand() != null) sb.append(item.getPhone().getBrand()).append(" ");
                        if (item.getPhone().getModel() != null) sb.append(item.getPhone().getModel()).append(" ");
                    }
                }
                String combined = sb.toString().toLowerCase();
                String combinedClean = combined.replaceAll("[^a-z0-9]", "");

                if (combined.contains(query)) return true;
                if (!queryClean.isEmpty() && combinedClean.contains(queryClean)) return true;

                if (tokens.length > 0) {
                    boolean allTokensMatch = true;
                    for (String token : tokens) {
                        if (token.isEmpty()) continue;
                        String tokenClean = token.replaceAll("[^a-z0-9]", "");
                        boolean tokenMatch = combined.contains(token)
                                || (!tokenClean.isEmpty() && combinedClean.contains(tokenClean));
                        if (!tokenMatch) {
                            allTokensMatch = false;
                            break;
                        }
                    }
                    return allTokensMatch;
                }
                return false;
            }).toList();
        }
        return sales;
    }

    @Transactional
    public void returnItem(Long saleId, Long itemId) {
        Sale sale = getSaleById(saleId);
        SaleItem itemToReturn = sale.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in this sale"));
                
        // Return phone to stock
        if (itemToReturn.getItemType() == ItemType.PHONE && itemToReturn.getPhone() != null) {
            Phone phone = itemToReturn.getPhone();
            phone.setStatus(PhoneStatus.RETURNED);
            phone.setSoldAt(null);
        }
        
        // Adjust sale totals
        BigDecimal itemSubtotal = itemToReturn.getUnitPrice().multiply(BigDecimal.valueOf(itemToReturn.getQuantity()));
        sale.setTotalAmount(sale.getTotalAmount().subtract(itemSubtotal));
        sale.setTotalProfit(sale.getTotalProfit().subtract(itemToReturn.getProfit()));
        
        // Remove item from sale
        sale.getItems().remove(itemToReturn);
        
        // If no items left, delete the sale entirely
        if (sale.getItems().isEmpty()) {
            saleRepository.delete(sale);
        } else {
            saleRepository.save(sale);
        }
    }

    @Transactional
    public void deleteSale(Long id) {
        Sale sale = getSaleById(id);
        for (SaleItem item : sale.getItems()) {
            if (item.getItemType() == ItemType.PHONE && item.getPhone() != null) {
                Phone phone = item.getPhone();
                phone.setStatus(PhoneStatus.RETURNED);
                phone.setSoldAt(null);
            }
        }
        saleRepository.delete(sale);
    }

    public BigDecimal getTodaySalesTotal() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        BigDecimal total = saleRepository.sumTotalAmountBetweenDates(startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTodayProfitTotal() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        BigDecimal profit = saleRepository.sumTotalProfitBetweenDates(startOfDay, endOfDay);
        return profit != null ? profit : BigDecimal.ZERO;
    }

    public long getTodaySalesCount() {
        return 0;
    }

    public List<Sale> getRecentSales(int limit) {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        List<Sale> recentSales = saleRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        if (recentSales.size() > limit) {
            return recentSales.subList(0, limit);
        }
        return recentSales;
    }

    private String generateInvoiceNumber() {
        return "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%03d", (int)(Math.random() * 1000));
    }
}
