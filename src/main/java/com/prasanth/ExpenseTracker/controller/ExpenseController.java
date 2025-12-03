package com.prasanth.ExpenseTracker.controller;

import com.prasanth.ExpenseTracker.model.Expense;
import com.prasanth.ExpenseTracker.model.User;
import com.prasanth.ExpenseTracker.repository.UserRepository;
import com.prasanth.ExpenseTracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    private User getUserFromPrincipal(Principal principal) {
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @PostMapping
    public Expense createExpense(Principal principal, @RequestBody Expense expense) {
        User user = getUserFromPrincipal(principal);
        return expenseService.saveExpense(expense, user);
    }

    @GetMapping
    public List<Expense> getAllExpenses(Principal principal) {
        User user = getUserFromPrincipal(principal);
        return expenseService.getAllExpensesByUserId(user.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(Principal principal, @PathVariable Long id) {
        User user = getUserFromPrincipal(principal);
        return expenseService.getExpenseByIdAndUserId(id, user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(Principal principal, @PathVariable Long id, @RequestBody Expense expenseDetails) {
        User user = getUserFromPrincipal(principal);
        return expenseService.getExpenseByIdAndUserId(id, user.getId())
                .map(expense -> {
                    expense.setTitle(expenseDetails.getTitle());
                    expense.setAmount(expenseDetails.getAmount());
                    expense.setCategory(expenseDetails.getCategory());
                    expense.setDate(expenseDetails.getDate());
                    expense.setDescription(expenseDetails.getDescription());
                    return ResponseEntity.ok(expenseService.saveExpense(expense, user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(Principal principal, @PathVariable Long id) {
        User user = getUserFromPrincipal(principal);
        try {
            expenseService.deleteExpense(id, user.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/filter")
    public List<Expense> filterExpenses(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String category) {
        User user = getUserFromPrincipal(principal);
        return expenseService.filterExpenses(user.getId(), dateFrom, dateTo, category);
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<Map<String, Double>> getMonthlySummary(Principal principal) {
        User user = getUserFromPrincipal(principal);
        Map<String, Double> summary = expenseService.getMonthlySummary(user.getId());
        return ResponseEntity.ok(summary);
    }
}
