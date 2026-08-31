package com.lingolearn.repository;

import com.lingolearn.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    Optional<StudyRecord> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);

    List<StudyRecord> findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(Long userId, LocalDate from, LocalDate to);

    /** 最近 N 天记录（倒序），用于计算连续打卡 */
    List<StudyRecord> findTop365ByUserIdOrderByStudyDateDesc(Long userId);

    @Query("select coalesce(sum(r.minutes),0), coalesce(sum(r.wordsLearned),0) " +
            "from StudyRecord r where r.user.id = :uid")
    List<Object[]> sumTotals(@Param("uid") Long userId);
}