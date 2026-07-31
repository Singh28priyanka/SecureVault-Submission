package com.example.securevault.repository;

import com.example.securevault.entity.Credential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository
        extends JpaRepository<Credential, Long>, JpaSpecificationExecutor<Credential> {

    List<Credential> findByUserIdAndDeletedFalse(Long userId);

    long countByUserIdAndDeletedFalse(Long userId);

    @Query("""
            SELECT c FROM Credential c
            JOIN FETCH c.user
            LEFT JOIN FETCH c.category
            WHERE c.user.id = :userId AND c.deleted = false
            """)
    List<Credential> findByUserIdWithUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT c FROM Credential c WHERE c.id = :id AND c.deleted = false")
    Optional<Credential> findActiveWithUserById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT c FROM Credential c WHERE c.id = :id AND c.deleted = true")
    Optional<Credential> findDeletedWithUserById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT c FROM Credential c WHERE c.id = :id")
    Optional<Credential> findWithUserById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "category"})
    Page<Credential> findAll(Specification<Credential> spec, Pageable pageable);
}
