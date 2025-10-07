package it.odvsicilia.backend.repository;

import it.odvsicilia.backend.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    
    List<ContactMessage> findByIsReadFalseOrderByCreatedAtDesc();
    
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT COUNT(c) FROM ContactMessage c WHERE c.isRead = false")
    long countUnreadMessages();
    
    List<ContactMessage> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
