package com.lingolearn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.*;
import com.lingolearn.entity.*;
import com.lingolearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** 课程与课时服务 */
@Service
public class CourseService {

    private final LanguageRepository languageRepository;
    private final CourseRepository courseRepository;
    private final UnitRepository unitRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final VipService vipService;
    private final ObjectMapper objectMapper;

    public CourseService(LanguageRepository languageRepository, CourseRepository courseRepository,
                         UnitRepository unitRepository, LessonRepository lessonRepository,
                         LessonProgressRepository progressRepository, VipService vipService,
                         ObjectMapper objectMapper) {
        this.languageRepository = languageRepository;
        this.courseRepository = courseRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.vipService = vipService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Language> listLanguages() {
        return languageRepository.findAllByOrderBySortOrderAsc();
    }

    /** 课程列表；languageCode 为空时返回全部语种课程 */
    @Transactional(readOnly = true)
    public List<CourseVO> listCourses(String languageCode, Long userId) {
        List<Course> courses;
        if (languageCode == null || languageCode.trim().isEmpty()) {
            List<Language> languages = languageRepository.findAllByOrderBySortOrderAsc();
            courses = new ArrayList<>();
            for (Language lang : languages) {
                courses.addAll(courseRepository.findByLanguageCodeOrderBySortOrderAsc(lang.getCode()));
            }
        } else {
            courses = courseRepository.findByLanguageCodeOrderBySortOrderAsc(languageCode.trim());
        }
        List<CourseVO> result = new ArrayList<>();
        for (Course course : courses) {
            result.add(toCourseVO(course, userId));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CourseVO toCourseVO(Course course, Long userId) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setLanguageId(course.getLanguage().getId());
        vo.setLanguageCode(course.getLanguage().getCode());
        vo.setLanguageName(course.getLanguage().getNameCn());
        vo.setTitle(course.getTitle());
        vo.setLevel(course.getLevel());
        vo.setLevelName(course.getLevelName());
        vo.setDescription(course.getDescription());
        vo.setCover(course.getCover());
        vo.setUnitCount(course.getUnitCount());
        vo.setLessonCount(course.getLessonCount() != null && course.getLessonCount() > 0
                ? course.getLessonCount() : (int) lessonRepository.countByUnitCourseId(course.getId()));
        // 游客（未登录）没有学习进度
        List<LessonProgress> progresses = userId == null ? new ArrayList<>()
                : progressRepository.findByUserIdAndLessonUnitCourseId(userId, course.getId());
        int completed = 0;
        int inProgress = 0;
        for (LessonProgress p : progresses) {
            if (LessonProgress.STATUS_COMPLETED.equals(p.getStatus())) {
                completed++;
            } else if (LessonProgress.STATUS_IN_PROGRESS.equals(p.getStatus())) {
                inProgress++;
            }
        }
        vo.setCompletedLessons(completed);
        vo.setInProgressLessons(inProgress);
        int total = vo.getLessonCount() == null || vo.getLessonCount() == 0 ? 1 : vo.getLessonCount();
        vo.setProgressPercent((int) Math.round(completed * 100.0 / total));
        // 方言语种标记：前端据此显示 VIP 角标与「近似发音」提示
        vo.setVipOnly(Boolean.TRUE.equals(course.getLanguage().getVipOnly()));
        vo.setTtsApproximate(Boolean.TRUE.equals(course.getLanguage().getTtsApproximate()));
        return vo;
    }

    /** 课程详情：单元 + 课时 + 我的进度（VIP 专属语种先校验权限） */
    @Transactional(readOnly = true)
    public Map<String, Object> courseDetail(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(404, "课程不存在"));
        vipService.assertLanguageAccess(userId, course.getLanguage());
        List<UnitVO> unitVOs = new ArrayList<>();
        List<Unit> units = unitRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        for (Unit unit : units) {
            UnitVO unitVO = new UnitVO();
            unitVO.setId(unit.getId());
            unitVO.setTitle(unit.getTitle());
            unitVO.setDescription(unit.getDescription());
            List<LessonBriefVO> lessonVOs = new ArrayList<>();
            for (Lesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                LessonBriefVO lv = new LessonBriefVO();
                lv.setId(lesson.getId());
                lv.setTitle(lesson.getTitle());
                lv.setType(lesson.getType());
                lv.setSortOrder(lesson.getSortOrder());
                LessionProgressHolder holder = findProgress(userId, lesson.getId());
                lv.setStatus(holder.status);
                lv.setBestScore(holder.bestScore);
                lessonVOs.add(lv);
            }
            unitVO.setLessons(lessonVOs);
            unitVOs.add(unitVO);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("course", toCourseVO(course, userId));
        result.put("units", unitVOs);
        return result;
    }

    /** 课时详情（含学习内容）；VIP 专属语种先校验权限 */
    @Transactional(readOnly = true)
    public LessonDetailVO lessonDetail(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(404, "课时不存在"));
        vipService.assertLanguageAccess(userId, lesson.getUnit().getCourse().getLanguage());
        LessonDetailVO vo = new LessonDetailVO();
        vo.setId(lesson.getId());
        vo.setTitle(lesson.getTitle());
        vo.setType(lesson.getType());
        Unit unit = lesson.getUnit();
        Course course = unit.getCourse();
        vo.setCourseId(course.getId());
        vo.setCourseTitle(course.getTitle());
        vo.setUnitId(unit.getId());
        vo.setUnitTitle(unit.getTitle());
        vo.setLanguageCode(course.getLanguage().getCode());
        LessionProgressHolder holder = findProgress(userId, lessonId);
        vo.setStatus(holder.status);
        vo.setBestScore(holder.bestScore);
        try {
            vo.setContent(objectMapper.readValue(lesson.getContentJson(), Object.class));
        } catch (Exception e) {
            throw new BusinessException(500, "课时内容解析失败");
        }
        return vo;
    }

    private LessionProgressHolder findProgress(Long userId, Long lessonId) {
        LessionProgressHolder holder = new LessionProgressHolder();
        if (userId == null) {
            return holder;
        }
        progressRepository.findByUserIdAndLessonId(userId, lessonId).ifPresent(p -> {
            holder.status = p.getStatus();
            holder.bestScore = p.getBestScore();
        });
        return holder;
    }

    private static class LessionProgressHolder {
        String status = "NOT_STARTED";
        Integer bestScore;
    }
}