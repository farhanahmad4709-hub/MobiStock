package com.mobistock.service;

import com.mobistock.dto.PhoneDTO;
import com.mobistock.model.Phone;
import com.mobistock.model.User;
import com.mobistock.model.enums.PhoneStatus;
import com.mobistock.repository.PhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneService {
    private final PhoneRepository repository;
    
    private final Set<String> catalogBrands = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> catalogModels = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> catalogColors = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> catalogStorageRam = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    @PostConstruct
    public void initCatalog() {
        try (InputStream is = getClass().getResourceAsStream("/phone_catalog.tsv")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                boolean isFirstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    String[] parts = line.split("\\t");
                    if (parts.length >= 4) {
                        catalogBrands.add(parts[0].trim());
                        catalogModels.add(parts[1].trim());
                        
                        String[] colors = parts[2].split(",");
                        for (String color : colors) {
                            catalogColors.add(color.trim());
                        }
                        
                        String[] storages = parts[3].split(",");
                        for (String storage : storages) {
                            catalogStorageRam.add(storage.trim());
                        }
                    }
                }
                log.info("Loaded phone catalog: {} brands, {} models", catalogBrands.size(), catalogModels.size());
            } else {
                log.warn("phone_catalog.tsv not found");
            }
        } catch (Exception e) {
            log.error("Failed to load phone catalog", e);
        }
    }

    public List<Phone> getAllPhones() {
        return repository.findAll();
    }

    public List<Phone> getInStockPhones() {
        return repository.findByStatusOrderByAddedAtDesc(PhoneStatus.IN_STOCK);
    }

    public Phone getPhoneById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Phone not found"));
    }

    public Optional<Phone> getPhoneByImei(String imei) {
        return repository.findByImei(imei);
    }

    @Transactional
    public void addPhone(PhoneDTO dto, User addedBy) {
        String[] imeis = dto.getImei().split("[,\\n\\r]+");
        for (String imeiRaw : imeis) {
            String imei = imeiRaw.trim();
            if (imei.isEmpty()) continue;
            
            if (isImeiExists(imei)) {
                throw new RuntimeException("IMEI already exists: " + imei);
            }
            Phone phone = new Phone();
            mapDtoToEntity(dto, phone);
            phone.setImei(imei);
            phone.setStatus(PhoneStatus.IN_STOCK);
            phone.setAddedBy(addedBy);
            phone.setAddedAt(LocalDateTime.now());
            repository.save(phone);
        }
    }

    @Transactional
    public Phone updatePhone(Long id, PhoneDTO dto) {
        Phone phone = getPhoneById(id);
        if (!phone.getImei().equals(dto.getImei()) && isImeiExists(dto.getImei())) {
            throw new RuntimeException("IMEI already exists");
        }
        mapDtoToEntity(dto, phone);
        return repository.save(phone);
    }

    @Transactional
    public void deletePhone(Long id) {
        repository.deleteById(id);
    }

    public List<Phone> searchPhones(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllPhones();
        }
        List<Phone> all = getAllPhones();
        String query = searchTerm.trim().toLowerCase();
        String queryClean = query.replaceAll("[^a-z0-9]", "");
        String[] tokens = query.split("[\\s,;/-]+");

        return all.stream()
                .filter(phone -> matchesPhone(phone, query, queryClean, tokens))
                .toList();
    }

    private boolean matchesPhone(Phone phone, String query, String queryClean, String[] tokens) {
        String brand = phone.getBrand() != null ? phone.getBrand().toLowerCase() : "";
        String model = phone.getModel() != null ? phone.getModel().toLowerCase() : "";
        String imei = phone.getImei() != null ? phone.getImei().toLowerCase() : "";
        String color = phone.getColor() != null ? phone.getColor().toLowerCase() : "";
        String storageRam = phone.getStorageRam() != null ? phone.getStorageRam().toLowerCase() : "";
        String condNotes = phone.getConditionNotes() != null ? phone.getConditionNotes().toLowerCase() : "";
        String intNotes = phone.getInternalNotes() != null ? phone.getInternalNotes().toLowerCase() : "";

        // Raw combined string with spaces: e.g. "samsung galaxy a34 864129460090001 blue 8/128"
        String combined = String.join(" ", brand, model, imei, color, storageRam, condNotes, intNotes);

        // 1. Direct substring match (e.g. "34", "sa", "samsung", "galaxy a34", "864129")
        if (combined.contains(query)) {
            return true;
        }

        // Clean combined string with alphanumeric only: e.g. "samsunggalaxya34864129460090001blue8128"
        String combinedClean = combined.replaceAll("[^a-z0-9]", "");

        // 2. Normalized substring match (e.g. "a 34" -> "a34", "8 128" -> "8128", "galaxy-a34" -> "galaxya34")
        if (!queryClean.isEmpty() && combinedClean.contains(queryClean)) {
            return true;
        }

        // 3. Multi-token / all-words match (e.g. "samsung a 34" -> ["samsung", "a", "34"], or "samsung a34" -> ["samsung", "a34"])
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
            if (allTokensMatch) {
                return true;
            }
        }

        return false;
    }

    public long countInStock() {
        return repository.countByStatus(PhoneStatus.IN_STOCK) + repository.countByStatus(PhoneStatus.RETURNED);
    }

    public boolean isImeiExists(String imei) {
        return repository.existsByImei(imei);
    }

    private void mapDtoToEntity(PhoneDTO dto, Phone phone) {
        phone.setImei(dto.getImei());
        phone.setBrand(dto.getBrand());
        phone.setModel(dto.getModel());
        phone.setStorageRam(dto.getStorageRam());
        phone.setColor(dto.getColor());
        phone.setCondition(dto.getCondition());
        phone.setConditionNotes(dto.getConditionNotes());
        phone.setInternalNotes(dto.getInternalNotes());
        phone.setCostPrice(dto.getCostPrice());
        phone.setSellingPrice(dto.getSellingPrice());
    }

    public List<String> getDistinctBrands() { 
        Set<String> combined = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        combined.addAll(catalogBrands);
        combined.addAll(repository.findDistinctBrands());
        return combined.stream().toList();
    }
    public List<String> getDistinctModels() {
        Set<String> combined = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        combined.addAll(catalogModels);
        combined.addAll(repository.findDistinctModels());
        return combined.stream().toList();
    }
    public List<String> getDistinctStorageRam() {
        Set<String> combined = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        combined.addAll(catalogStorageRam);
        combined.addAll(repository.findDistinctStorageRam());
        return combined.stream().toList();
    }
    public List<String> getDistinctColors() {
        Set<String> combined = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        combined.addAll(catalogColors);
        combined.addAll(repository.findDistinctColors());
        return combined.stream().toList();
    }
}
