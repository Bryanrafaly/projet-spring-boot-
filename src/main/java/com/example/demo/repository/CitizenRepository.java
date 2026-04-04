package com.example.demo.repository;

import com.example.demo.model.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CitizenRepository extends JpaRepository<Citizen, Long> {

    Optional<Citizen> findByNationalNumber(String nationalNumber);

    List<Citizen> findByArchivedFalse();

    long countByArchivedFalse();

    List<Citizen> findByLastNameContainingIgnoreCaseOrNationalNumberContainingIgnoreCase(String lastName, String nationalNumber);
}
