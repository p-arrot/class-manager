-- ============================================
-- test-data.sql - 测试数据（可重复执行，幂等）
-- 用法: docker exec -i edu-postgres psql -U edu -d edu < test-data.sql
-- ============================================

-- 1. 学校
INSERT INTO schools (name)
  SELECT '阳光实验小学'
  WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name = '阳光实验小学');

-- 2. 班级（幂等：已存在则跳过，不存在则插入）
DO $$ DECLARE school_id BIGINT := (SELECT id FROM schools WHERE name = '阳光实验小学' LIMIT 1);
BEGIN
  IF NOT EXISTS (SELECT 1 FROM school_classes WHERE grade = '2024' AND name = '1班' AND deleted = 0) THEN
    INSERT INTO school_classes (school_id, grade, name) VALUES (school_id, '2024', '1班');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM school_classes WHERE grade = '2024' AND name = '2班' AND deleted = 0) THEN
    INSERT INTO school_classes (school_id, grade, name) VALUES (school_id, '2024', '2班');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM school_classes WHERE grade = '2023' AND name = '1班' AND deleted = 0) THEN
    INSERT INTO school_classes (school_id, grade, name) VALUES (school_id, '2023', '1班');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM school_classes WHERE grade = '2022' AND name = '1班' AND deleted = 0) THEN
    INSERT INTO school_classes (school_id, grade, name) VALUES (school_id, '2022', '1班');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM school_classes WHERE grade = '2022' AND name = '2班' AND deleted = 0) THEN
    INSERT INTO school_classes (school_id, grade, name) VALUES (school_id, '2022', '2班');
  END IF;
END $$;

-- 3. 教师账号（密码: teacher123）
INSERT INTO users (username, name, password, role, enabled)
  SELECT 'zhang', '张老师', crypt('teacher123', gen_salt('bf')), 'teacher', true
  WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'zhang');
INSERT INTO users (username, name, password, role, enabled)
  SELECT 'li', '李老师', crypt('teacher123', gen_salt('bf')), 'teacher', true
  WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'li');
INSERT INTO users (username, name, password, role, enabled)
  SELECT 'wang', '王老师', crypt('teacher123', gen_salt('bf')), 'teacher', true
  WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'wang');
INSERT INTO users (username, name, password, role, enabled)
  SELECT 'chen', '陈老师', crypt('teacher123', gen_salt('bf')), 'teacher', true
  WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'chen');

-- 4. 教师-班级绑定
DO $$ DECLARE
  zhang_id BIGINT := (SELECT id FROM users WHERE username = 'zhang');
  li_id    BIGINT := (SELECT id FROM users WHERE username = 'li');
  wang_id  BIGINT := (SELECT id FROM users WHERE username = 'wang');
  chen_id  BIGINT := (SELECT id FROM users WHERE username = 'chen');
  c31_id   BIGINT := (SELECT id FROM school_classes WHERE grade = '2024' AND name = '1班' AND deleted = 0 LIMIT 1);
  c32_id   BIGINT := (SELECT id FROM school_classes WHERE grade = '2024' AND name = '2班' AND deleted = 0 LIMIT 1);
  c41_id   BIGINT := (SELECT id FROM school_classes WHERE grade = '2023' AND name = '1班' AND deleted = 0 LIMIT 1);
  c51_id   BIGINT := (SELECT id FROM school_classes WHERE grade = '2022' AND name = '1班' AND deleted = 0 LIMIT 1);
  c52_id   BIGINT := (SELECT id FROM school_classes WHERE grade = '2022' AND name = '2班' AND deleted = 0 LIMIT 1);
BEGIN
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (zhang_id, c31_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (zhang_id, c32_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (li_id,    c41_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (wang_id,  c51_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (wang_id,  c52_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (chen_id,  c31_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (chen_id,  c41_id) ON CONFLICT DO NOTHING;
  INSERT INTO teacher_classes (teacher_id, class_id) VALUES (chen_id,  c51_id) ON CONFLICT DO NOTHING;
END $$;

-- 5. 学生账号（密码: 123456）
DO $$ DECLARE
  c31_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2024' AND name = '1班' AND deleted = 0 LIMIT 1);
  c32_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2024' AND name = '2班' AND deleted = 0 LIMIT 1);
  c41_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2023' AND name = '1班' AND deleted = 0 LIMIT 1);
  c51_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2022' AND name = '1班' AND deleted = 0 LIMIT 1);
  c52_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2022' AND name = '2班' AND deleted = 0 LIMIT 1);
BEGIN
  -- 2024级1班 (10人)
  INSERT INTO users (student_no, name, password, role, class_id, enabled) VALUES
    ('2024001','赵一凡',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024002','钱晓明',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024003','孙思远',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024004','李小雨',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024005','周浩然',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024006','吴思琪',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024007','郑天宇',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024008','王梓涵',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024009','冯雨桐',crypt('123456',gen_salt('bf')),'student',c31_id,true),
    ('2024010','陈子轩',crypt('123456',gen_salt('bf')),'student',c31_id,true)
  ON CONFLICT DO NOTHING;
  -- 2024级2班 (8人)
  INSERT INTO users (student_no, name, password, role, class_id, enabled) VALUES
    ('2024011','杨睿',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024012','朱晓雯',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024013','秦天',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024014','许可欣',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024015','何俊杰',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024016','吕思源',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024017','张晨',crypt('123456',gen_salt('bf')),'student',c32_id,true),
    ('2024018','曹阳',crypt('123456',gen_salt('bf')),'student',c32_id,true)
  ON CONFLICT DO NOTHING;
  -- 2023级1班 (8人)
  INSERT INTO users (student_no, name, password, role, class_id, enabled) VALUES
    ('2023016','金浩宇',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023017','魏思远',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023018','陶然',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023019','姜雨辰',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023020','戚悦',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023021','谢天佑',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023022','邹雨萱',crypt('123456',gen_salt('bf')),'student',c41_id,true),
    ('2023023','柏睿',crypt('123456',gen_salt('bf')),'student',c41_id,true)
  ON CONFLICT DO NOTHING;
  -- 2022级1班 (8人)
  INSERT INTO users (student_no, name, password, role, class_id, enabled) VALUES
    ('2022021','苏小曼',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022022','潘子涵',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022023','葛铭',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022024','范思哲',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022025','彭博',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022026','鲁宇轩',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022027','马思源',crypt('123456',gen_salt('bf')),'student',c51_id,true),
    ('2022028','方圆',crypt('123456',gen_salt('bf')),'student',c51_id,true)
  ON CONFLICT DO NOTHING;
  -- 2022级2班 (8人)
  INSERT INTO users (student_no, name, password, role, class_id, enabled) VALUES
    ('2022029','史晨',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022030','唐婉儿',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022031','费翔',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022032','廉明',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022033','岑雨',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022034','薛之谦',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022035','雷震',crypt('123456',gen_salt('bf')),'student',c52_id,true),
    ('2022036','贺子秋',crypt('123456',gen_salt('bf')),'student',c52_id,true)
  ON CONFLICT DO NOTHING;
END $$;

-- 6. 课程
INSERT INTO courses (name, description, teacher_id)
  SELECT 'Python编程基础', '面向小学生的Python编程入门课程。', (SELECT id FROM users WHERE username = 'zhang')
  WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Python编程基础' AND teacher_id = (SELECT id FROM users WHERE username = 'zhang'));
INSERT INTO courses (name, description, teacher_id)
  SELECT '机器人入门', '乐高EV3机器人编程，学习传感器与电机控制。', (SELECT id FROM users WHERE username = 'li')
  WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = '机器人入门' AND teacher_id = (SELECT id FROM users WHERE username = 'li'));
INSERT INTO courses (name, description, teacher_id)
  SELECT '信息技术基础', '计算机基础知识、办公软件使用、网络素养。', (SELECT id FROM users WHERE username = 'wang')
  WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = '信息技术基础' AND teacher_id = (SELECT id FROM users WHERE username = 'wang'));
INSERT INTO courses (name, description, teacher_id)
  SELECT 'Scratch创意编程', '通过Scratch学习编程逻辑，制作动画和小游戏。', (SELECT id FROM users WHERE username = 'chen')
  WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Scratch创意编程' AND teacher_id = (SELECT id FROM users WHERE username = 'chen'));
INSERT INTO courses (name, description, teacher_id)
  SELECT '3D打印与设计', '学习Tinkercad建模，了解3D打印原理。', (SELECT id FROM users WHERE username = 'chen')
  WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = '3D打印与设计' AND teacher_id = (SELECT id FROM users WHERE username = 'chen'));

-- 7. 课程-班级绑定
DO $$ DECLARE
  py_id  BIGINT := (SELECT id FROM courses WHERE name = 'Python编程基础' AND teacher_id = (SELECT id FROM users WHERE username = 'zhang'));
  rb_id  BIGINT := (SELECT id FROM courses WHERE name = '机器人入门' AND teacher_id = (SELECT id FROM users WHERE username = 'li'));
  it_id  BIGINT := (SELECT id FROM courses WHERE name = '信息技术基础' AND teacher_id = (SELECT id FROM users WHERE username = 'wang'));
  sc_id  BIGINT := (SELECT id FROM courses WHERE name = 'Scratch创意编程' AND teacher_id = (SELECT id FROM users WHERE username = 'chen'));
  td_id  BIGINT := (SELECT id FROM courses WHERE name = '3D打印与设计' AND teacher_id = (SELECT id FROM users WHERE username = 'chen'));
  c31_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2024' AND name = '1班' AND deleted = 0 LIMIT 1);
  c32_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2024' AND name = '2班' AND deleted = 0 LIMIT 1);
  c41_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2023' AND name = '1班' AND deleted = 0 LIMIT 1);
  c51_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2022' AND name = '1班' AND deleted = 0 LIMIT 1);
  c52_id BIGINT := (SELECT id FROM school_classes WHERE grade = '2022' AND name = '2班' AND deleted = 0 LIMIT 1);
BEGIN
  INSERT INTO course_classes (course_id, class_id) VALUES
    (py_id, c51_id), (py_id, c52_id),                           -- Python: 五年级
    (rb_id, c41_id), (rb_id, c51_id),                           -- 机器人: 四、五年级
    (it_id, c31_id), (it_id, c32_id), (it_id, c41_id),         -- 信息技术: 三、四年级
    (sc_id, c31_id), (sc_id, c32_id),                           -- Scratch: 三年级
    (td_id, c51_id), (td_id, c52_id)                            -- 3D打印: 五年级
  ON CONFLICT DO NOTHING;
END $$;

-- 8. 学期
DO $$ DECLARE
  py_id BIGINT := (SELECT id FROM courses WHERE name = 'Python编程基础' AND teacher_id = (SELECT id FROM users WHERE username = 'zhang'));
  rb_id BIGINT := (SELECT id FROM courses WHERE name = '机器人入门' AND teacher_id = (SELECT id FROM users WHERE username = 'li'));
  it_id BIGINT := (SELECT id FROM courses WHERE name = '信息技术基础' AND teacher_id = (SELECT id FROM users WHERE username = 'wang'));
  sc_id BIGINT := (SELECT id FROM courses WHERE name = 'Scratch创意编程' AND teacher_id = (SELECT id FROM users WHERE username = 'chen'));
BEGIN
  -- Python: 秋季 + 春季
  INSERT INTO semesters (name, start_time, end_time, course_id) VALUES
    ('2026年秋季学期', '2026-09-01 00:00:00', '2027-01-15 00:00:00', py_id),
    ('2027年春季学期', '2027-02-15 00:00:00', '2027-06-30 00:00:00', py_id)
  ON CONFLICT DO NOTHING;
  -- 机器人: 秋季
  INSERT INTO semesters (name, start_time, end_time, course_id) VALUES
    ('2026年秋季学期', '2026-09-01 00:00:00', '2027-01-15 00:00:00', rb_id)
  ON CONFLICT DO NOTHING;
  -- 信息技术: 秋季
  INSERT INTO semesters (name, start_time, end_time, course_id) VALUES
    ('2026年秋季学期', '2026-09-01 00:00:00', '2027-01-15 00:00:00', it_id)
  ON CONFLICT DO NOTHING;
  -- Scratch: 秋季
  INSERT INTO semesters (name, start_time, end_time, course_id) VALUES
    ('2026年秋季学期', '2026-09-01 00:00:00', '2027-01-15 00:00:00', sc_id)
  ON CONFLICT DO NOTHING;
END $$;

-- 9. 课时
DO $$ DECLARE
  py_fall_id BIGINT := (SELECT id FROM semesters WHERE name = '2026年秋季学期' AND course_id = (SELECT id FROM courses WHERE name = 'Python编程基础' LIMIT 1));
  py_spr_id  BIGINT := (SELECT id FROM semesters WHERE name = '2027年春季学期' AND course_id = (SELECT id FROM courses WHERE name = 'Python编程基础' LIMIT 1));
  rb_fall_id BIGINT := (SELECT id FROM semesters WHERE name = '2026年秋季学期' AND course_id = (SELECT id FROM courses WHERE name = '机器人入门' LIMIT 1));
  it_fall_id BIGINT := (SELECT id FROM semesters WHERE name = '2026年秋季学期' AND course_id = (SELECT id FROM courses WHERE name = '信息技术基础' LIMIT 1));
  sc_fall_id BIGINT := (SELECT id FROM semesters WHERE name = '2026年秋季学期' AND course_id = (SELECT id FROM courses WHERE name = 'Scratch创意编程' LIMIT 1));
BEGIN
  -- Python 秋季 (8课时)
  INSERT INTO lessons (name, sort_order, semester_id) VALUES
    ('第1课：认识Python', 1, py_fall_id), ('第2课：变量与数据类型', 2, py_fall_id),
    ('第3课：条件判断', 3, py_fall_id), ('第4课：for循环', 4, py_fall_id),
    ('第5课：while循环', 5, py_fall_id), ('第6课：列表与元组', 6, py_fall_id),
    ('第7课：函数入门', 7, py_fall_id), ('第8课：turtle绘图', 8, py_fall_id)
  ON CONFLICT DO NOTHING;
  -- Python 春季 (6课时)
  INSERT INTO lessons (name, sort_order, semester_id) VALUES
    ('第1课：复习与提高', 1, py_spr_id), ('第2课：字典与集合', 2, py_spr_id),
    ('第3课：文件读写', 3, py_spr_id), ('第4课：面向对象入门', 4, py_spr_id),
    ('第5课：简单GUI编程', 5, py_spr_id), ('第6课：综合项目', 6, py_spr_id)
  ON CONFLICT DO NOTHING;
  -- 机器人 秋季 (6课时)
  INSERT INTO lessons (name, sort_order, semester_id) VALUES
    ('第1课：认识EV3套件', 1, rb_fall_id), ('第2课：马达控制', 2, rb_fall_id),
    ('第3课：触碰传感器', 3, rb_fall_id), ('第4课：超声波传感器', 4, rb_fall_id),
    ('第5课：颜色传感器', 5, rb_fall_id), ('第6课：巡线机器人', 6, rb_fall_id)
  ON CONFLICT DO NOTHING;
  -- 信息技术 秋季 (7课时)
  INSERT INTO lessons (name, sort_order, semester_id) VALUES
    ('第1课：认识计算机', 1, it_fall_id), ('第2课：键盘与鼠标操作', 2, it_fall_id),
    ('第3课：Windows基础', 3, it_fall_id), ('第4课：文件管理', 4, it_fall_id),
    ('第5课：Word文字处理', 5, it_fall_id), ('第6课：PPT演示文稿', 6, it_fall_id),
    ('第7课：网络素养与安全', 7, it_fall_id)
  ON CONFLICT DO NOTHING;
  -- Scratch 秋季 (6课时)
  INSERT INTO lessons (name, sort_order, semester_id) VALUES
    ('第1课：认识Scratch界面', 1, sc_fall_id), ('第2课：角色与背景', 2, sc_fall_id),
    ('第3课：运动与方向', 3, sc_fall_id), ('第4课：事件与广播', 4, sc_fall_id),
    ('第5课：变量与计分', 5, sc_fall_id), ('第6课：综合项目——迷宫游戏', 6, sc_fall_id)
  ON CONFLICT DO NOTHING;
END $$;

-- 10. 课程资源文件夹
DO $$ DECLARE
  py_id BIGINT := (SELECT id FROM courses WHERE name = 'Python编程基础' LIMIT 1);
  it_id BIGINT := (SELECT id FROM courses WHERE name = '信息技术基础' LIMIT 1);
  sc_id BIGINT := (SELECT id FROM courses WHERE name = 'Scratch创意编程' LIMIT 1);
BEGIN
  INSERT INTO course_resources (course_id, parent_id, name, type, sort_order) VALUES
    (py_id, NULL, '课件资料', 'FOLDER', 1),
    (py_id, NULL, '学生作品', 'FOLDER', 2),
    (it_id, NULL, '课件资料', 'FOLDER', 1),
    (it_id, NULL, '练习素材', 'FOLDER', 2),
    (sc_id, NULL, '教学资源', 'FOLDER', 1)
  ON CONFLICT DO NOTHING;
END $$;

-- ============================================
-- 验证
-- ============================================
SELECT '=== 测试数据统计 ===' AS "";
SELECT '班级' AS 类型, count(*) AS 数量 FROM school_classes WHERE deleted = 0
UNION ALL SELECT '教师', count(*) FROM users WHERE role = 'teacher' AND deleted = 0
UNION ALL SELECT '学生', count(*) FROM users WHERE role = 'student' AND deleted = 0
UNION ALL SELECT '教师-班级绑定', count(*) FROM teacher_classes
UNION ALL SELECT '课程', count(*) FROM courses WHERE deleted = 0
UNION ALL SELECT '课程-班级绑定', count(*) FROM course_classes
UNION ALL SELECT '学期', count(*) FROM semesters WHERE deleted = 0
UNION ALL SELECT '课时', count(*) FROM lessons WHERE deleted = 0
UNION ALL SELECT '课程资源文件夹', count(*) FROM course_resources WHERE deleted = 0;

SELECT '=== 测试账号 ===' AS "";
SELECT 'admin / admin123 (管理员)' AS 账号
UNION ALL SELECT 'zhang / teacher123 (教师-张老师，负责2024级1/2班)'
UNION ALL SELECT 'li / teacher123 (教师-李老师，负责2023级1班)'
UNION ALL SELECT 'wang / teacher123 (教师-王老师，负责2022级1/2班)'
UNION ALL SELECT 'chen / teacher123 (教师-陈老师，负责2024/2023/2022级1班)'
UNION ALL SELECT '2024001 / 123456 (学生-赵一凡，2024级1班)'
UNION ALL SELECT '2024011 / 123456 (学生-杨睿，2024级2班)'
UNION ALL SELECT '2023016 / 123456 (学生-金浩宇，2023级1班)'
UNION ALL SELECT '2022021 / 123456 (学生-苏小曼，2022级1班)'
UNION ALL SELECT '2022029 / 123456 (学生-史晨，2022级2班)';
