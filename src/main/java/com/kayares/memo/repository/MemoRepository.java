package com.kayares.memo.repository;

import com.kayares.memo.domain.Memo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    List<Memo> findAll();

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Memo> findById(Long id);
}