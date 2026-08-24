package com.example.securevault.repository;

import com.example.securevault.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    List<PasswordHistory> findTop5ByCredential_IdOrderByVersionDesc(Long credentialId);

    Optional<PasswordHistory> findTopByCredential_IdOrderByVersionDesc(Long credentialId);

    void deleteByCredential_Id(Long credentialId);

    long countByCredential_Id(Long credentialId);
}
