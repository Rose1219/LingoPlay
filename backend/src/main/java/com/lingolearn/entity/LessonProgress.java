package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/** 课时学习进度 */
@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
})
@Data
public class LessonProgress {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false, length = 20)
    private String status;

    /** 最佳得分（0-100） */
    @Column(name = "best_score")
    private Integer bestScore;

    /** 完成次数 */
    @Column(name = "total_attempts")
    private Integer totalAttempts = 0;

    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;
}