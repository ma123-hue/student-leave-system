INSERT IGNORE INTO student_profiles
    (student_id, student_name, gender, college, grade, major, class_name)
VALUES
    ('2024000001', '张三', '男', '信息工程学院', '2024级', '软件工程', '软件工程2401班'),
    ('2024000002', '李四', '女', '信息工程学院', '2024级', '计算机科学与技术', '计科2401班');

INSERT IGNORE INTO student_accounts
    (student_id, password_hash, password_salt, must_change_password)
VALUES
    ('2024000001', '378c2aa9839d05458ca2adde3c873566c58fe24a2a34d4c41dc5f34ed532b2b0', 'student001salt', 1),
    ('2024000002', '41d2243eb970c00735ea841d59169ce1e0b3cd70d820c35975fcb855e3e71ccc', 'student002salt', 1);

INSERT IGNORE INTO admin_accounts
    (admin_id, admin_name, password_hash, password_salt, role, active)
VALUES
    ('admin001', '测试辅导员', '822938fa9f16319f8721afb99e26691efa6f86ef0f760df5a1269092573ae221', 'admin001salt', '辅导员', 1);

INSERT IGNORE INTO admin_role_settings (id, current_role)
VALUES (1, '辅导员');
