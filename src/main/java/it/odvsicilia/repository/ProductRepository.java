package it.odvsicilia.repository;

import it.odvsicilia.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStockGreaterThan(Integer stock);
    List<Product> findByCategory(String category);
    
    @Query("SELECT p FROM Product p WHERE p.stock > 0 ORDER BY p.createdAt DESC")
    List<Product> findAvailableProductsOrderedByDate();
}

