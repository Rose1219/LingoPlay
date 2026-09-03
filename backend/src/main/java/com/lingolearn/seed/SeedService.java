package com.lingolearn.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingolearn.entity.*;
import com.lingolearn.repository.*;
import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/** 首次启动时初始化种子数据（课程内容、成就、演示账号、示例帖子） */
@Component
public class SeedService implements CommandLineRunner {

    private final LanguageRepository languageRepository;
    private final CourseRepository courseRepository;
    private final UnitRepository unitRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AchievementRepository achievementRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public SeedService(LanguageRepository languageRepository, CourseRepository courseRepository,
                       UnitRepository unitRepository, LessonRepository lessonRepository,
                       UserRepository userRepository, PostRepository postRepository,
                       AchievementRepository achievementRepository, PasswordEncoder passwordEncoder,
                       ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.languageRepository = languageRepository;
        this.courseRepository = courseRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.achievementRepository = achievementRepository;
        this.passwordEncoder = passwordEncoder;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        SeedMeta meta = objectMapper.readValue(readResource("seed/meta.json"), SeedMeta.class);
        boolean firstInit = languageRepository.count() == 0;

        // 语种每次启动都做增量同步：新上线的语种（法/西/阿/中/方言）自动补进老库，
        // 已有语种只刷新展示字段，不动主键与关联数据（幂等）
        syncLanguages(meta);

        if (firstInit) {
            // 成就定义
            List<Achievement> achievements = objectMapper.readValue(
                    readResource("seed/achievements.json"), new TypeReference<List<Achievement>>() {
                    });
            for (Achievement a : achievements) {
                achievementRepository.save(a);
            }
            // 演示账号
            User demo = new User();
            demo.setUsername(meta.getDemoUser().getUsername());
            demo.setEmail(meta.getDemoUser().getEmail());
            demo.setPassword(passwordEncoder.encode(meta.getDemoUser().getPassword()));
            demo.setNickname(meta.getDemoUser().getNickname());
            demo.setAvatar(meta.getDemoUser().getAvatar());
            demo.setPreferredLanguages(meta.getDemoUser().getPreferredLanguages());
            userRepository.save(demo);

            // 示例帖子
            int index = 0;
            for (PostSeed ps : meta.getPosts()) {
                Post post = new Post();
                post.setUser(demo);
                if (ps.getLanguage() != null && !ps.getLanguage().isEmpty()) {
                    post.setLanguage(languageRepository.findByCode(ps.getLanguage())
                            .orElseThrow(() -> new IllegalStateException("未知语种: " + ps.getLanguage())));
                }
                post.setTitle(ps.getTitle());
                post.setContent(ps.getContent());
                post.setLikeCount(ps.getLikeCount());
                post.setCommentCount(ps.getCommentCount());
                post.setCreatedAt(LocalDateTime.now().minusDays(2).minusHours(index * 6L));
                postRepository.save(post);
                index++;
            }
        }

        // 课程内容增量同步：新增的课程/单元/课时自动补齐，已有的不动（幂等）
        syncCourses(meta);
    }

    /** 语种增量同步：缺失即插入；已存在则刷新名称/描述/排序/VIP 标记 */
    private void syncLanguages(SeedMeta meta) {
        for (Language seed : meta.getLanguages()) {
            Language existing = languageRepository.findByCode(seed.getCode()).orElse(null);
            if (existing == null) {
                languageRepository.save(seed);
                continue;
            }
            existing.setName(seed.getName());
            existing.setNameCn(seed.getNameCn());
            existing.setIcon(seed.getIcon());
            existing.setDescription(seed.getDescription());
            existing.setSortOrder(seed.getSortOrder());
            existing.setVipOnly(Boolean.TRUE.equals(seed.getVipOnly()));
            existing.setTtsApproximate(Boolean.TRUE.equals(seed.getTtsApproximate()));
            existing.setFallbackTo(seed.getFallbackTo());
            languageRepository.save(existing);
        }
    }

    /**
     * 课程体系增量同步：
     * 按「课程标题+语种」「单元标题+课程」「课时标题+单元」逐级判重，
     * 仅插入缺失内容并刷新课程统计，保证老库升级后新词库自动生效。
     */
    private void syncCourses(SeedMeta meta) throws Exception {
        for (CourseSeed cs : meta.getCourses()) {
            Language lang = languageRepository.findByCode(cs.getLanguage())
                    .orElseThrow(() -> new IllegalStateException("未知语种: " + cs.getLanguage()));
            Course course = courseRepository.findByLanguageIdAndTitle(lang.getId(), cs.getTitle())
                    .orElseGet(() -> {
                        Course c = new Course();
                        c.setLanguage(lang);
                        c.setTitle(cs.getTitle());
                        return c;
                    });
            course.setLevel(cs.getLevel());
            course.setLevelName(cs.getLevelName());
            course.setDescription(cs.getDescription());
            course.setCover(cs.getCover());
            course.setSortOrder(cs.getSortOrder());
            // 先保存课程：新建课程持久化拿到主键后，其下 Unit 才能安全引用
            courseRepository.save(course);

            for (UnitSeed us : cs.getUnits()) {
                Unit unit = unitRepository.findByCourseIdAndTitle(course.getId() != null ? course.getId() : -1L, us.getTitle())
                        .orElseGet(() -> {
                            Unit u = new Unit();
                            u.setCourse(course);
                            u.setTitle(us.getTitle());
                            return u;
                        });
                unit.setDescription(us.getDescription());
                unit.setSortOrder(us.getSortOrder());
                unitRepository.save(unit);

                for (LessionSeed ls : us.getLessons()) {
                    boolean exists = unit.getId() != null
                            && lessonRepository.existsByUnitIdAndTitle(unit.getId(), ls.getTitle());
                    if (exists) {
                        continue;
                    }
                    String content = objectMapper
                            .readTree(readResource("seed/lessons/" + ls.getContentFile())).toString();
                    Lesson lesson = new Lesson();
                    lesson.setUnit(unit);
                    lesson.setTitle(ls.getTitle());
                    lesson.setType(ls.getType());
                    lesson.setSortOrder(ls.getSortOrder());
                    lesson.setContentJson(content);
                    lessonRepository.save(lesson);
                }
            }

            // 刷新课程统计（单元数与课时数以库内实际为准）
            List<Unit> units = unitRepository.findByCourseIdOrderBySortOrderAsc(course.getId());
            course.setUnitCount(units.size());
            int lessonTotal = 0;
            for (Unit u : units) {
                lessonTotal += lessonRepository.findByUnitIdOrderBySortOrderAsc(u.getId()).size();
            }
            course.setLessonCount(lessonTotal);
            courseRepository.save(course);
        }
    }

    private InputStream readResource(String path) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        return resource.getInputStream();
    }

    // ---------------- 种子数据模型 ----------------

    @Data
    public static class SeedMeta {
        private List<Language> languages;
        private DemoUserSeed demoUser;
        private List<PostSeed> posts;
        private List<CourseSeed> courses;
    }

    @Data
    public static class DemoUserSeed {
        private String username;
        private String email;
        private String password;
        private String nickname;
        private String avatar;
        private String preferredLanguages;
    }

    @Data
    public static class PostSeed {
        private String language;
        private String title;
        private String content;
        private Integer likeCount;
        private Integer commentCount;
    }

    @Data
    public static class CourseSeed {
        private String language;
        private String title;
        private String level;
        private String levelName;
        private String description;
        private String cover;
        private Integer sortOrder;
        private List<UnitSeed> units;
    }

    @Data
    public static class UnitSeed {
        private String title;
        private String description;
        private Integer sortOrder;
        private List<LessionSeed> lessons;
    }

    @Data
    public static class LessionSeed {
        private String title;
        private String type;
        private Integer sortOrder;
        private String contentFile;
    }
}