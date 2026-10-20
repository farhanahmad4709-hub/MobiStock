package com.mobistock.dto;

import com.mobistock.model.enums.PhoneCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PhoneDTO {
    @NotBlank
    private String imei;
    @NotBlank
    private String brand;
    @NotBlank
    private String model;
    private String storageRam;
    private String color;
    private PhoneCondition condition;
    private String conditionNotes;
    private String internalNotes;
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal costPrice;
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal sellingPrice;
}
