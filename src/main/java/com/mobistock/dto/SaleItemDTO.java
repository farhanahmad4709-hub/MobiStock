package com.mobistock.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SaleItemDTO {
    private String itemType;
    private Long phoneId;

    private int quantity;
    private BigDecimal unitPrice;
}
