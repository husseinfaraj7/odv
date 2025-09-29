package it.odvsicilia.backend.repository;

import it.odvsicilia.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items ORDER BY o.createdAt DESC")
    List<Order> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = it.odvsicilia.backend.model.Order$OrderStatus.PENDING")
    long countPendingOrders();
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = it.odvsicilia.backend.model.Order$PaymentStatus.PENDING")
    long countPendingPayments();
    
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
