package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

/** 每日学习记录（用于连续打卡、学习热力图） */
@Entity
@Table(name = "study_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "study_date"})
})
@Data
public class StudyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    /** 当日学习分钟数 */
    @Column(nullable = false)
    private Integer minutes = 0;

    @Column(name = "words_learned")
    private Integer wordsLearned = 0;

    @Column(name = "questions_answered")
    private Integer questionsAnswered = 0;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;
}