package com.lingolearn.repository;

import com.lingolearn.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long> {

    List<Language> findAllByOrderBySortOrderAsc();

    Optional<Language> findByCode(String code);
}