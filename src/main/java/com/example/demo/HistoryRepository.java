package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<HistoryRecord, Long> {

    // Custom query to find history items only for the active logged-in user
    List<HistoryRecord> findByUsername(String username);
}