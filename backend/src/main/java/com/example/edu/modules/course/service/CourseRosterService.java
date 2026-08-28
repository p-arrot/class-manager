package com.example.edu.modules.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseRosterService {

    private final CourseClassMapper courseClassMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final UserMapper userMapper;

    public CourseRoster load(Long courseId) {
        return load(courseId, null);
    }

    public CourseRoster load(Long courseId, Long classId) {
        List<CourseClass> bindings = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, courseId));
        Set<Long> classIds = bindings.stream()
                .map(CourseClass::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (classId != null) {
            if (!classIds.contains(classId)) throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            classIds = Set.of(classId);
        }
        if (classIds.isEmpty()) return CourseRoster.empty();

        List<User> students = Optional.ofNullable(userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "student")
                        .in(User::getClassId, classIds)))
                .orElse(List.of());
        List<SchoolClass> classes = Optional.ofNullable(schoolClassMapper.selectBatchIds(classIds)).orElse(List.of());
        return CourseRoster.of(students, classes);
    }

    public record CourseRoster(List<User> students, Map<Long, SchoolClass> classes) {
        public static CourseRoster empty() {
            return new CourseRoster(List.of(), Map.of());
        }

        public static CourseRoster of(List<User> students, List<SchoolClass> classes) {
            Map<Long, SchoolClass> classMap = classes.stream()
                    .collect(Collectors.toMap(SchoolClass::getId, Function.identity(), (left, right) -> left));
            List<User> sortedStudents = students.stream()
                    .sorted(Comparator
                            .comparing((User user) -> Optional.ofNullable(displayName(classMap.get(user.getClassId()))).orElse(""))
                            .thenComparing(user -> Optional.ofNullable(user.getStudentNo()).orElse(""))
                            .thenComparing(user -> Optional.ofNullable(user.getName()).orElse("")))
                    .toList();
            return new CourseRoster(sortedStudents, Map.copyOf(classMap));
        }

        public SchoolClass schoolClass(Long classId) {
            return classId == null ? null : classes.get(classId);
        }

        public String displayClassName(Long classId) {
            return displayName(schoolClass(classId));
        }

        private static String displayName(SchoolClass schoolClass) {
            if (schoolClass == null) return null;
            return Optional.ofNullable(schoolClass.getGrade()).orElse("")
                    + "级"
                    + Optional.ofNullable(schoolClass.getName()).orElse("");
        }
    }
}
