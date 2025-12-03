package com.prasanth.ExpenseTracker.service;

import com.prasanth.ExpenseTracker.model.Expense;
import com.prasanth.ExpenseTracker.model.User;
import com.prasanth.ExpenseTracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public List<Expense> getAllExpensesByUserId(Long userId) {
        return expenseRepository.findByUserId(userId);
    }

    public Optional<Expense> getExpenseByIdAndUserId(Long id, Long userId) {
        return expenseRepository.findById(id).filter(expense -> expense.getUser().getId().equals(userId));
    }

    public Expense saveExpense(Expense expense, User user) {
        expense.setUser(user);
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id, Long userId) {
        Expense expense = expenseRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Expense not found or does not belong to user"));
        expenseRepository.delete(expense);
    }

    public List<Expense> filterExpenses(Long userId, LocalDate dateFrom, LocalDate dateTo, String category) {
        Specification<Expense> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), dateTo));
            }
            if (category != null && !category.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return expenseRepository.findAll(spec);
    }

    public Map<String, Double> getMonthlySummary(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getDate().getYear() + "-" + String.format("%02d", expense.getDate().getMonthValue()),
                        Collectors.summingDouble(Expense::getAmount)
                ));
    }
}
