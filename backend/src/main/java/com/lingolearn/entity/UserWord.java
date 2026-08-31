package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/** 用户单词本（记忆追踪） */
@Entity
@Table(name = "user_words", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "language_id", "word"})
})
@Data
public class UserWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(length = 255)
    private String meaning;

    /** 掌握度 0-4：0 新学 → 4 已掌握 */
    @Column(nullable = false)
    private Integer mastery = 0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "correct_streak")
    private Integer correctStreak = 0;
}