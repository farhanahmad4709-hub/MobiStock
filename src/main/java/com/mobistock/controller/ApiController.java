package com.mobistock.controller;

import com.mobistock.model.Phone;
import com.mobistock.model.enums.PhoneStatus;
import com.mobistock.repository.PhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final PhoneRepository phoneRepository;

    @GetMapping("/lookup")
    public ResponseEntity<?> lookupItem(@RequestParam String code) {
        // First check phone by IMEI
        Phone phone = phoneRepository.findAll().stream()
                .filter(p -> p.getImei().equalsIgnoreCase(code) && (p.getStatus() == PhoneStatus.IN_STOCK || p.getStatus() == PhoneStatus.RETURNED))
                .findFirst().orElse(null);
        
        if (phone != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", phone.getId());
            response.put("type", "PHONE");
            response.put("name", phone.getBrand() + " " + phone.getModel() + " (" + phone.getStorageRam() + ")");
            response.put("imei", phone.getImei());
            response.put("price", phone.getSellingPrice());
            response.put("cost", phone.getCostPrice());
            response.put("maxQty", 1);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }
}
