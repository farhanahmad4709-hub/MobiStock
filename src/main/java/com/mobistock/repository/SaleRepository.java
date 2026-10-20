package com.mobistock.repository;

import com.mobistock.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.createdAt BETWEEN :start AND :end")
    BigDecimal sumTotalAmountBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(s.totalProfit) FROM Sale s WHERE s.createdAt BETWEEN :start AND :end")
    BigDecimal sumTotalProfitBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Sale s SET s.soldBy = :newSoldBy WHERE s.soldBy = :oldSoldBy")
    void reassignSales(@Param("oldSoldBy") com.mobistock.model.User oldSoldBy, @Param("newSoldBy") com.mobistock.model.User newSoldBy);
}
