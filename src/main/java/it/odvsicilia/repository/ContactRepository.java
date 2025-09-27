package it.odvsicilia.repository;

import it.odvsicilia.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByStatusOrderByCreatedAtDesc(String status);
    List<Contact> findAllByOrderByCreatedAtDesc();
}
