package com.example.edu.modules.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.service.CourseRosterService.CourseRoster;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRosterServiceTest {

    @Mock private CourseClassMapper courseClassMapper;
    @Mock private SchoolClassMapper schoolClassMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks private CourseRosterService service;

    @Test
    void loadsBoundClassesAndSortsStudentsByClassAndStudentNumber() {
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                binding(1L, 11L), binding(1L, 10L)));
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                student(3L, 11L, "2026002", "吴三"),
                student(2L, 10L, "2026002", "周二"),
                student(1L, 10L, "2026001", "林一")));
        when(schoolClassMapper.selectBatchIds(any())).thenReturn(List.of(
                schoolClass(10L, "2026", "1班"), schoolClass(11L, "2026", "2班")));

        CourseRoster roster = service.load(1L);

        assertThat(roster.students()).extracting(User::getId).containsExactly(1L, 2L, 3L);
        assertThat(roster.displayClassName(10L)).isEqualTo("2026级1班");
    }

    @Test
    void rejectsClassFilterOutsideCourseBindings() {
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(1L, 10L)));

        assertThatThrownBy(() -> service.load(1L, 99L))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
        verify(userMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void returnsEmptyRosterWhenCourseHasNoClasses() {
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        CourseRoster roster = service.load(1L);

        assertThat(roster.students()).isEmpty();
        verify(userMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    private static CourseClass binding(Long courseId, Long classId) {
        CourseClass binding = new CourseClass();
        binding.setCourseId(courseId);
        binding.setClassId(classId);
        return binding;
    }

    private static User student(Long id, Long classId, String studentNo, String name) {
        User user = new User();
        user.setId(id);
        user.setRole("student");
        user.setClassId(classId);
        user.setStudentNo(studentNo);
        user.setName(name);
        return user;
    }

    private static SchoolClass schoolClass(Long id, String grade, String name) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade(grade);
        schoolClass.setName(name);
        return schoolClass;
    }
}
