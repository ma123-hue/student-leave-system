package com.example.studentleave.controller;

import com.example.studentleave.common.Result;
import com.example.studentleave.util.PasswordUtils;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final JdbcTemplate jdbcTemplate;

    public AdminAuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
        if (request == null || !StringUtils.hasText(request.getAdminId())
                || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "请输入管理员账号和密码");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT admin_id, admin_name, password_hash, password_salt, role, active "
                        + "FROM admin_accounts WHERE admin_id = ? LIMIT 1",
                request.getAdminId());
        if (rows.isEmpty() || !isActive(rows.get(0)) || !passwordMatches(request.getPassword(), rows.get(0))) {
            return Result.error(401, "管理员账号或密码错误");
        }

        Map<String, Object> account = rows.get(0);
        session.removeAttribute("studentId");
        session.setAttribute("adminId", String.valueOf(account.get("admin_id")));
        return Result.success("登录成功", adminData(account));
    }

    @GetMapping("/session")
    public Result<Map<String, Object>> session(HttpSession session) {
        Object adminId = session.getAttribute("adminId");
        if (adminId == null) {
            return Result.error(401, "请先登录管理端");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT admin_id, admin_name, role, active FROM admin_accounts WHERE admin_id = ? LIMIT 1",
                String.valueOf(adminId));
        if (rows.isEmpty() || !isActive(rows.get(0))) {
            session.removeAttribute("adminId");
            return Result.error(401, "管理端登录状态已失效");
        }
        return Result.success(adminData(rows.get(0)));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.removeAttribute("adminId");
        return Result.success();
    }

    private boolean passwordMatches(String password, Map<String, Object> account) {
        String encoded = PasswordUtils.sha256Password(password, String.valueOf(account.get("password_salt")));
        return encoded.equalsIgnoreCase(String.valueOf(account.get("password_hash")));
    }

    private boolean isActive(Map<String, Object> account) {
        Object active = account.get("active");
        return active != null && Boolean.parseBoolean(String.valueOf(active))
                || active instanceof Number && ((Number) active).intValue() == 1;
    }

    private Map<String, Object> adminData(Map<String, Object> account) {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> admin = new HashMap<String, Object>();
        admin.put("adminId", account.get("admin_id"));
        admin.put("name", account.get("admin_name"));
        admin.put("role", account.get("role"));
        data.put("admin", admin);
        return data;
    }

    @Data
    public static class LoginRequest {
        private String adminId;
        private String password;
    }
}
