package com.example.studentleave.controller;

import com.example.studentleave.common.Result;
import com.example.studentleave.entity.StudentAccount;
import com.example.studentleave.entity.StudentProfile;
import com.example.studentleave.mapper.StudentAccountMapper;
import com.example.studentleave.mapper.StudentProfileMapper;
import com.example.studentleave.util.PasswordUtils;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生认证控制器
 * 处理学生登录、会话校验、密码修改、登出等认证相关接口
 */
@RestController
@RequestMapping("/api/student")
public class AuthController {

    // 学生账户数据访问层
    private final StudentAccountMapper studentAccountMapper;
    // 学生档案数据访问层
    private final StudentProfileMapper studentProfileMapper;
    // 安全随机数生成器，用于生成密码盐值
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 构造函数注入依赖
     * @param studentAccountMapper 学生账户Mapper
     * @param studentProfileMapper 学生档案Mapper
     */
    public AuthController(StudentAccountMapper studentAccountMapper, StudentProfileMapper studentProfileMapper) {
        this.studentAccountMapper = studentAccountMapper;
        this.studentProfileMapper = studentProfileMapper;
    }

    /**
     * 学生登录接口
     * @param request 登录请求体（学号、密码）
     * @param session HTTP会话，用于存储登录状态
     * @return 登录结果，包含学生信息和登录状态
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
        // 参数校验：学号和密码不能为空
        if (!StringUtils.hasText(request.getStudentId()) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "请输入学号和密码");
        }

        // 查询学生账户信息
        StudentAccount account = studentAccountMapper.selectById(request.getStudentId());
        // 账户不存在或密码校验失败
        if (account == null || !checkPassword(request.getPassword(), account)) {
            return Result.error(401, "学号或密码错误");
        }

        // 查询学生档案信息
        StudentProfile profile = studentProfileMapper.selectById(account.getStudentId());
        // 会话中存储学号，标记登录状态
        session.setAttribute("studentId", account.getStudentId());

        // 构建返回的学生数据
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("student", buildStudentData(account, profile));

        // 首次登录需强制修改密码
        if (Boolean.TRUE.equals(account.getMustChangePassword())) {
            return new Result<Map<String, Object>>(403, "首次登录请先修改密码", data);
        }

        return Result.success("登录成功", data);
    }

    /**
     * 校验登录会话有效性接口
     * @param session HTTP会话，获取当前登录学号
     * @return 会话有效返回学生信息，无效返回未登录提示
     */
    @GetMapping("/session")
    public Result<Map<String, Object>> session(HttpSession session) {
        try {
            // 获取当前登录的学生账户
            StudentAccount account = currentAccount(session);
            StudentProfile profile = studentProfileMapper.selectById(account.getStudentId());
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("student", buildStudentData(account, profile));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 学生密码修改接口
     * @param request 密码修改请求体（当前密码、新密码）
     * @param session HTTP会话，获取当前登录学生
     * @return 密码修改结果，包含更新后的学生信息
     */
    @PostMapping("/change-password")
    public Result<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request, HttpSession session) {
        try {
            // 获取当前登录的学生账户
            StudentAccount account = currentAccount(session);

            // 校验当前密码是否为空
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                return Result.error(400, "请输入当前密码");
            }
            // 校验当前密码是否正确
            if (!checkPassword(request.getCurrentPassword(), account)) {
                return Result.error(400, "当前密码不正确");
            }
            // 校验新密码长度（6-32位）
            if (!StringUtils.hasText(request.getNewPassword())
                    || request.getNewPassword().length() < 6
                    || request.getNewPassword().length() > 32) {
                return Result.error(400, "新密码长度需要为6到32位");
            }
            // 禁止使用初始密码123456
            if ("123456".equals(request.getNewPassword())) {
                return Result.error(400, "新密码不能继续使用初始密码");
            }

            // 生成新的盐值并加密新密码
            String salt = randomHex(12);
            account.setPasswordSalt(salt);
            account.setPasswordHash(PasswordUtils.sha256Password(request.getNewPassword(), salt));
            // 取消强制修改密码标记
            account.setMustChangePassword(false);
            // 更新账户密码信息
            studentAccountMapper.updateById(account);

            // 构建返回的学生数据
            StudentProfile profile = studentProfileMapper.selectById(account.getStudentId());
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("student", buildStudentData(account, profile));
            return Result.success("密码修改成功", data);
        } catch (Exception e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 学生登出接口
     * @param session HTTP会话，销毁当前会话
     * @return 登出成功结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        // 销毁会话，清除登录状态
        session.invalidate();
        return Result.success();
    }

    /**
     * 从会话中获取当前登录的学生账户
     * @param session HTTP会话
     * @return 学生账户信息
     * @throws IllegalStateException 会话中无学号或账户不存在时抛出异常
     */
    private StudentAccount currentAccount(HttpSession session) {
        Object studentId = session.getAttribute("studentId");
        if (studentId == null) {
            throw new IllegalStateException("请先登录");
        }
        StudentAccount account = studentAccountMapper.selectById(String.valueOf(studentId));
        if (account == null) {
            throw new IllegalStateException("登录状态已失效，请重新登录");
        }
        return account;
    }

    /**
     * 校验密码是否正确
     * @param rawPassword 原始密码（用户输入）
     * @param account 学生账户（存储加密密码和盐值）
     * @return 密码匹配返回true，否则返回false
     */
    private boolean checkPassword(String rawPassword, StudentAccount account) {
        // 加密原始密码并与数据库中存储的哈希值比对
        String encoded = PasswordUtils.sha256Password(rawPassword, account.getPasswordSalt());
        return encoded.equalsIgnoreCase(account.getPasswordHash());
    }

    /**
     * 构建返回给前端的学生数据
     * @param account 学生账户信息
     * @param profile 学生档案信息
     * @return 整合后的学生数据Map
     */
    private Map<String, Object> buildStudentData(StudentAccount account, StudentProfile profile) {
        Map<String, Object> student = new HashMap<String, Object>();
        student.put("id", account.getStudentId());
        student.put("studentId", account.getStudentId());
        student.put("mustChangePassword", Boolean.TRUE.equals(account.getMustChangePassword()));
        // 补充学生档案信息（姓名、性别、学院等）
        if (profile != null) {
            student.put("name", profile.getStudentName());
            student.put("studentName", profile.getStudentName());
            student.put("gender", profile.getGender());
            student.put("college", profile.getCollege());
            student.put("grade", profile.getGrade());
            student.put("major", profile.getMajor());
            student.put("className", profile.getClassName());
        }
        return student;
    }

    /**
     * 生成指定长度的十六进制随机字符串（用于密码盐值）
     * @param bytesLength 字节长度
     * @return 十六进制随机字符串
     */
    private String randomHex(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        secureRandom.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytesLength * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    /**
     * 登录请求体封装类
     */
    @Data
    public static class LoginRequest {
        // 学生学号
        private String studentId;
        // 学生密码
        private String password;
    }

    /**
     * 密码修改请求体封装类
     */
    @Data
    public static class ChangePasswordRequest {
        // 当前密码
        private String currentPassword;
        // 新密码
        private String newPassword;
    }
}