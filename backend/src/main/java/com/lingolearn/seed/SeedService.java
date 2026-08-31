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
        if (languageRepository.count() > 0) {
            return; // 已初始化，幂等跳过
        }
        SeedMeta meta = objectMapper.readValue(readResource("seed/meta.json"), SeedMeta.class);

        // 语种
        for (Language lang : meta.getLanguages()) {
            languageRepository.save(lang);
        }
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

        // 课程体系
        for (CourseSeed cs : meta.getCourses()) {
            Language lang = languageRepository.findByCode(cs.getLanguage())
                    .orElseThrow(() -> new IllegalStateException("未知语种: " + cs.getLanguage()));
            Course course = new Course();
            course.setLanguage(lang);
            course.setTitle(cs.getTitle());
            course.setLevel(cs.getLevel());
            course.setLevelName(cs.getLevelName());
            course.setDescription(cs.getDescription());
            course.setCover(cs.getCover());
            course.setSortOrder(cs.getSortOrder());
            course.setUnitCount(cs.getUnits().size());
            int lessonTotal = 0;
            for (UnitSeed us : cs.getUnits()) {
                lessonTotal += us.getLessons().size();
            }
            course.setLessonCount(lessonTotal);
            courseRepository.save(course);

            for (UnitSeed us : cs.getUnits()) {
                Unit unit = new Unit();
                unit.setCourse(course);
                unit.setTitle(us.getTitle());
                unit.setDescription(us.getDescription());
                unit.setSortOrder(us.getSortOrder());
                unitRepository.save(unit);

                for (LessionSeed ls : us.getLessons()) {
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
        }

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