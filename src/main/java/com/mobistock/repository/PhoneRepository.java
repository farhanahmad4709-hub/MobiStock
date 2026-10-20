package com.mobistock.repository;

import com.mobistock.model.Phone;
import com.mobistock.model.enums.PhoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, Long> {
    Optional<Phone> findByImei(String imei);
    boolean existsByImei(String imei);
    List<Phone> findByStatus(PhoneStatus status);
    List<Phone> findByBrandAndModel(String brand, String model);
    List<Phone> findByStatusOrderByAddedAtDesc(PhoneStatus status);
    long countByStatus(PhoneStatus status);

    @Query("SELECT p FROM Phone p WHERE LOWER(p.brand) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(p.model) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(CONCAT(p.brand, ' ', p.model)) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR p.imei LIKE CONCAT('%', :term, '%')")
    List<Phone> searchPhones(@Param("term") String term);

    @Query("SELECT DISTINCT p.brand FROM Phone p WHERE p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    @Query("SELECT DISTINCT p.model FROM Phone p WHERE p.model IS NOT NULL ORDER BY p.model")
    List<String> findDistinctModels();
    
    @Query("SELECT DISTINCT p.storageRam FROM Phone p WHERE p.storageRam IS NOT NULL ORDER BY p.storageRam")
    List<String> findDistinctStorageRam();
    
    @Query("SELECT DISTINCT p.color FROM Phone p WHERE p.color IS NOT NULL ORDER BY p.color")
    List<String> findDistinctColors();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Phone p SET p.addedBy = :newAddedBy WHERE p.addedBy = :oldAddedBy")
    void reassignPhones(@Param("oldAddedBy") com.mobistock.model.User oldAddedBy, @Param("newAddedBy") com.mobistock.model.User newAddedBy);
}
