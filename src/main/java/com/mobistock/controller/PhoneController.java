package com.mobistock.controller;

import com.mobistock.dto.PhoneDTO;
import com.mobistock.service.PhoneService;
import com.mobistock.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/phones")
@RequiredArgsConstructor
public class PhoneController {
    private final PhoneService phoneService;
    private final UserService userService;

    @GetMapping
    public String listPhones(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "condition", required = false) String condition,
            Model model) {
        
        java.util.List<com.mobistock.model.Phone> phones;
        
        if (q != null && !q.trim().isEmpty()) {
            phones = phoneService.searchPhones(q.trim());
        } else {
            phones = phoneService.getAllPhones();
        }
        
        if (status != null && !status.isEmpty()) {
            phones = phones.stream()
                .filter(p -> p.getStatus().name().equalsIgnoreCase(status))
                .collect(java.util.stream.Collectors.toList());
        }
        
        if (condition != null && !condition.isEmpty()) {
            phones = phones.stream()
                .filter(p -> p.getCondition() != null && p.getCondition().name().equalsIgnoreCase(condition))
                .collect(java.util.stream.Collectors.toList());
        }
        
        model.addAttribute("phones", phones);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCondition", condition);
        model.addAttribute("searchQuery", q);
        model.addAttribute("pageTitle", "Phones Inventory");
        return "phones/list";
    }

    @GetMapping("/add")
    public String addPhoneForm(Model model) {
        model.addAttribute("phoneDTO", new PhoneDTO());
        model.addAttribute("pageTitle", "Add New Phone");
        populateDatalists(model);
        return "phones/add";
    }

    @PostMapping("/add")
    public String addPhone(@Valid @ModelAttribute PhoneDTO phoneDTO, BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Add New Phone");
            populateDatalists(model);
            return "phones/add";
        }
        try {
            phoneService.addPhone(phoneDTO, userService.getCurrentUser());
            redirectAttributes.addFlashAttribute("success", "Phone(s) added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/phones";
    }

    @GetMapping("/edit/{id}")
    public String editPhoneForm(@PathVariable Long id, Model model) {
        model.addAttribute("phone", phoneService.getPhoneById(id));
        model.addAttribute("pageTitle", "Edit Phone");
        populateDatalists(model);
        return "phones/edit";
    }

    @PostMapping("/edit/{id}")
    public String editPhone(@PathVariable Long id, @Valid @ModelAttribute PhoneDTO phoneDTO, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("phone", phoneService.getPhoneById(id));
            model.addAttribute("pageTitle", "Edit Phone");
            populateDatalists(model);
            return "phones/edit";
        }
        try {
            phoneService.updatePhone(id, phoneDTO);
            redirectAttributes.addFlashAttribute("success", "Phone updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/phones";
    }

    @PostMapping("/delete/{id}")
    public String deletePhone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            phoneService.deletePhone(id);
            redirectAttributes.addFlashAttribute("success", "Phone deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting phone");
        }
        return "redirect:/phones";
    }

    @GetMapping("/search")
    public String searchPhones(@RequestParam("q") String term, Model model) {
        model.addAttribute("phones", phoneService.searchPhones(term));
        model.addAttribute("pageTitle", "Phones Inventory");
        return "phones/list";
    }

    private void populateDatalists(Model model) {
        model.addAttribute("distinctBrands", phoneService.getDistinctBrands());
        model.addAttribute("distinctModels", phoneService.getDistinctModels());
        model.addAttribute("distinctStorageRam", phoneService.getDistinctStorageRam());
        model.addAttribute("distinctColors", phoneService.getDistinctColors());
    }
}
