package com.lingolearn.repository;

import com.lingolearn.entity.UserWord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWordRepository extends JpaRepository<UserWord, Long> {

    Optional<UserWord> findByUserIdAndLanguageIdAndWord(Long userId, Long languageId, String word);

    /** 用户某语种的全部单词本记录（用于过滤已出现过的词） */
    List<UserWord> findByUserIdAndLanguageId(Long userId, Long languageId);

    long countByUserId(Long userId);

    long countByUserIdAndMasteryGreaterThan(Long userId, int mastery);

    /** 未完全掌握的词，按复习时间从旧到新（用于安排复习） */
    List<UserWord> findByUserIdAndMasteryLessThanOrderByLastReviewedAtAsc(Long userId, int mastery, Pageable pageable);
}