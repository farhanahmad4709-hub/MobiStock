package com.mobistock.dto;

import lombok.Data;
import java.util.List;

@Data
public class SaleDTO {
    private String customerName;
    private String customerPhone;
    private String notes;
    private List<SaleItemDTO> items;
}
