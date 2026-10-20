package com.mobistock.model;

import com.mobistock.model.enums.PhoneCondition;
import com.mobistock.model.enums.PhoneStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 15)
    private String imei;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "storage_ram")
    private String storageRam;

    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`", nullable = false)
    private PhoneCondition condition;

    @Column(columnDefinition = "TEXT")
    private String conditionNotes;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PhoneStatus status = PhoneStatus.IN_STOCK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime addedAt;

    private LocalDateTime soldAt;
}
