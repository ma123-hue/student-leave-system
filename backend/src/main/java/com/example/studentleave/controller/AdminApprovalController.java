package com.example.studentleave.controller;

import com.example.studentleave.common.Result;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员审批控制器
 * 处理请假审批、返校审批、续假审批、角色管理、数据统计等管理员相关接口
 * @author 开发团队
 * @date 2024-XX-XX
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApprovalController {

    // 管理员可切换的角色列表
    private static final List<String> ROLES = Arrays.asList("辅导员", "学院分管副院长", "学院党委副书记");

    // JDBC模板，用于自定义SQL查询
    private final JdbcTemplate jdbcTemplate;
    // 学生请假控制器，复用请假详情查询逻辑
    private final StudentLeaveController studentLeaveController;

    /**
     * 构造函数注入依赖
     * @param jdbcTemplate JDBC模板
     * @param studentLeaveController 学生请假控制器
     */
    public AdminApprovalController(JdbcTemplate jdbcTemplate, StudentLeaveController studentLeaveController) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentLeaveController = studentLeaveController;
    }

    /**
     * 查询待审批列表接口
     * @param page 页码，默认1
     * @param size 每页条数，默认100
     * @param search 搜索关键词（学号/姓名）
     * @param type 审批类型（leave/return/overtime/ranking/extension）
     * @param filter 筛选条件（all/returned/not_returned）
     * @return 待审批列表数据，包含分页信息和统计数
     */
    @GetMapping("/approvals")
    public Result<Map<String, Object>> listTodo(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "100") Integer size,
                                                @RequestParam(required = false) String search,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false, defaultValue = "all") String filter) {
        // 查询符合条件的审批数据
        List<Map<String, Object>> rows = queryApprovals(StringUtils.hasText(type) ? type : "leave", search, filter);
        // 处理分页逻辑
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int pageSize = Math.max(size == null ? 100 : size, 1);
        int fromIndex = Math.min((currentPage - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());

        // 构建返回数据（分页+统计）
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("total", rows.size());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("records", rows.subList(fromIndex, toIndex));
        data.put("rows", rows.subList(fromIndex, toIndex));
        data.put("counts", getAdminCounts());
        return Result.success(data);
    }

    /**
     * 获取当前管理员角色接口
     * @return 当前角色和可选角色列表
     */
    @GetMapping("/role")
    public Result<Map<String, Object>> getRole() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("role", getAdminRole());
        data.put("roles", ROLES);
        return Result.success(data);
    }

    /**
     * 更新管理员角色接口
     * @param request 角色修改请求体（目标角色）
     * @return 角色修改结果，包含新角色和可选角色列表
     */
    @PostMapping("/role")
    public Result<Map<String, Object>> updateRole(@RequestBody RoleRequest request) {
        // 校验角色是否合法
        if (request == null || !ROLES.contains(request.getRole())) {
            return Result.error(400, "角色不存在");
        }
        // 插入/更新管理员角色配置
        jdbcTemplate.update(
                "INSERT INTO admin_role_settings (id, current_role) VALUES (1, ?) " +
                        "ON DUPLICATE KEY UPDATE current_role = VALUES(current_role)",
                request.getRole());

        // 构建返回数据
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("role", request.getRole());
        data.put("roles", ROLES);
        return Result.success("角色切换成功", data);
    }

    /**
     * 查询审批历史接口
     * @param scope 历史范围（mine/cc/all）
     * @param search 搜索关键词（学号/姓名）
     * @return 审批历史列表
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> history(@RequestParam(required = false, defaultValue = "all") String scope,
                                               @RequestParam(required = false, defaultValue = "") String search) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("rows", queryHistory(scope, search));
        return Result.success(data);
    }

    /**
     * 查询请假统计数据接口
     * @return 省份分布、请假类型、学院统计、留校/离校占比等数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(queryStatistics());
    }

    /**
     * 查询请假详情接口（管理员视角）
     * @param id 请假单ID
     * @return 请假单详情数据
     */
    @GetMapping("/leaves/{id}")
    public Result<Map<String, Object>> getLeaveDetail(@PathVariable Long id) {
        try {
            return Result.success(studentLeaveController.queryLeaveDetail(id, false, null));
        } catch (Exception e) {
            return Result.error(404, e.getMessage());
        }
    }

    /**
     * 审批通过请假申请接口
     * @param id 请假单ID
     * @return 审批结果
     */
    @PostMapping("/leaves/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> approveLeave(@PathVariable Long id) {
        try {
            return Result.success(approveLeaveFlow(id));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 拒绝请假申请接口
     * @param id 请假单ID
     * @param request 审批请求体（包含拒绝理由）
     * @return 审批结果
     */
    @PostMapping("/leaves/{id}/reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> rejectLeave(@PathVariable Long id, @RequestBody(required = false) ApprovalRequest request) {
        try {
            String comment = request == null ? null : request.getComment();
            return Result.success("已拒绝请假", rejectLeaveFlow(id, comment));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 审批通过返校申请接口
     * @param id 返校申请ID
     * @return 审批结果
     */
    @PostMapping("/returns/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> approveReturn(@PathVariable Long id) {
        try {
            // 查询返校申请关联的请假单
            Map<String, Object> request = singleRow(
                    "SELECT leave_id FROM return_requests WHERE id = ? AND status = 'pending' LIMIT 1",
                    id);
            // 更新返校申请状态为已通过
            jdbcTemplate.update("UPDATE return_requests SET status = 'approved' WHERE id = ?", id);
            // 更新对应请假单状态为已结束
            jdbcTemplate.update("UPDATE leave_applications SET status = 'ended' WHERE id = ?", request.get("leave_id"));

            // 构建返回数据
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("id", id);
            data.put("status", "approved");
            return Result.success("返校审批成功", data);
        } catch (Exception e) {
            return Result.error(400, "当前返校申请不存在或已处理");
        }
    }

    /**
     * 拒绝返校申请接口
     * @param id 返校申请ID
     * @param request 审批请求体（包含拒绝理由）
     * @return 审批结果
     */
    @PostMapping("/returns/{id}/reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> rejectReturn(@PathVariable Long id, @RequestBody(required = false) ApprovalRequest request) {
        // 更新返校申请状态为已拒绝，并记录拒绝理由
        int updated = jdbcTemplate.update(
                "UPDATE return_requests SET status = 'rejected', comment = ? WHERE id = ? AND status = 'pending'",
                request == null || !StringUtils.hasText(request.getComment()) ? "不同意返校" : request.getComment(),
                id);
        // 校验是否更新成功（申请不存在或已处理）
        if (updated == 0) {
            return Result.error(400, "当前返校申请不存在或已处理");
        }

        // 构建返回数据
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", id);
        data.put("status", "rejected");
        return Result.success("已拒绝返校", data);
    }

    /**
     * 审批通过续假申请接口
     * @param id 续假申请ID
     * @return 审批结果
     */
    @PostMapping("/extensions/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> approveExtension(@PathVariable Long id) {
        try {
            return Result.success(approveExtensionFlow(id));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 拒绝续假申请接口
     * @param id 续假申请ID
     * @param request 审批请求体（包含拒绝理由）
     * @return 审批结果
     */
    @PostMapping("/extensions/{id}/reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> rejectExtension(@PathVariable Long id, @RequestBody(required = false) ApprovalRequest request) {
        try {
            return Result.success("已拒绝续假", rejectExtensionFlow(id, request == null ? null : request.getComment()));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 通过请假单ID审批通过续假申请接口
     * @param id 请假单ID
     * @return 审批结果
     */
    @PostMapping("/leaves/{id}/extension-approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> approveExtensionByLeave(@PathVariable Long id) {
        // 查询该请假单最新的待审批续假申请
        Map<String, Object> request = latestExtensionByLeave(id);
        if (request == null) {
            return Result.error(404, "当前请假没有待审批续假申请");
        }
        // 审批续假申请
        return approveExtension(toLong(request.get("id")));
    }

    /**
     * 通过请假单ID拒绝续假申请接口
     * @param id 请假单ID
     * @param request 审批请求体（包含拒绝理由）
     * @return 审批结果
     */
    @PostMapping("/leaves/{id}/extension-reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> rejectExtensionByLeave(@PathVariable Long id,
                                                              @RequestBody(required = false) ApprovalRequest request) {
        // 查询该请假单最新的待审批续假申请
        Map<String, Object> extension = latestExtensionByLeave(id);
        if (extension == null) {
            return Result.error(404, "当前请假没有待审批续假申请");
        }
        // 拒绝续假申请
        return rejectExtension(toLong(extension.get("id")), request);
    }

    /**
     * 通用审批数据查询方法
     * @param type 审批类型（leave/return/overtime/ranking/extension）
     * @param search 搜索关键词
     * @param filter 筛选条件
     * @return 符合条件的审批数据列表
     */
    private List<Map<String, Object>> queryApprovals(String type, String search, String filter) {
        String like = "%" + (search == null ? "" : search) + "%";
        String approverRole = roleToApproverRole(getAdminRole());

        // 返校申请审批
        if ("return".equals(type)) {
            return jdbcTemplate.queryForList(
                    "SELECT rr.id AS return_id, rr.status AS return_status, rr.return_time, l.id, l.student_id, l.student_name, " +
                            "l.leave_type, l.reason, l.start_time, l.end_time, l.created_at " +
                            "FROM return_requests rr JOIN leave_applications l ON l.id = rr.leave_id " +
                            "WHERE rr.status = 'pending' AND (l.student_name LIKE ? OR l.student_id LIKE ?) ORDER BY rr.created_at ASC",
                    like, like);
        }

        // 超时未返校审批
        if ("overtime".equals(type)) {
            String returnFilter = "";
            if ("returned".equals(filter)) {
                returnFilter = " AND rr.status = 'approved' ";
            } else if ("not_returned".equals(filter)) {
                returnFilter = " AND (rr.id IS NULL OR rr.status <> 'approved') ";
            }
            return jdbcTemplate.queryForList(
                    "SELECT l.id, l.student_id, l.student_name, l.leave_type, l.reason, l.start_time, l.end_time, l.created_at, " +
                            "rr.status AS return_status, rr.return_time " +
                            "FROM leave_applications l " +
                            "LEFT JOIN return_requests rr ON rr.id = (SELECT id FROM return_requests WHERE leave_id = l.id ORDER BY created_at DESC LIMIT 1) " +
                            "WHERE l.status = 'approved' AND l.end_time < NOW() " + returnFilter +
                            "AND (l.student_name LIKE ? OR l.student_id LIKE ?) ORDER BY l.end_time ASC",
                    like, like);
        }

        // 请假次数排名
        if ("ranking".equals(type)) {
            return jdbcTemplate.queryForList(
                    "SELECT l.student_id, l.student_name, SUM(l.created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')) AS month_count, " +
                            "COUNT(*) AS year_count FROM leave_applications l " +
                            "WHERE l.created_at >= MAKEDATE(YEAR(NOW()), 1) AND (l.student_name LIKE ? OR l.student_id LIKE ?) " +
                            "GROUP BY l.student_id, l.student_name ORDER BY month_count DESC, l.student_id ASC LIMIT 20",
                    like, like);
        }

        // 续假申请审批
        if ("extension".equals(type)) {
            return queryPendingExtensions(like, approverRole);
        }

        // 默认：待审批请假+待审批续假
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.addAll(queryPendingLeaves(like, approverRole));
        rows.addAll(queryPendingExtensions(like, approverRole));
        return rows;
    }

    /**
     * 查询待审批请假申请
     * @param like 模糊查询关键词（学号/姓名）
     * @param approverRole 当前管理员审批角色
     * @return 待审批请假申请列表
     */
    private List<Map<String, Object>> queryPendingLeaves(String like, String approverRole) {
        String bareRole = bareApproverRole(approverRole);
        return jdbcTemplate.queryForList(
                "SELECT l.id, NULL AS extension_id, 'leave' AS approval_kind, l.student_id, l.student_name, l.leave_type, " +
                        "l.reason, l.start_time, l.end_time, l.approval_level, l.approval_level AS total_steps, l.created_at, " +
                        "s.step_no, s.approver_role, s.approver_name, s.status AS step_status " +
                        "FROM leave_applications l JOIN approval_steps s ON s.leave_id = l.id AND s.status = 'pending' " +
                        "WHERE l.status = 'pending' AND s.approver_role IN (?, ?) AND (l.student_name LIKE ? OR l.student_id LIKE ?) " +
                        "ORDER BY l.created_at ASC",
                approverRole, bareRole, like, like);
    }

    /**
     * 查询待审批续假申请
     * @param like 模糊查询关键词（学号/姓名）
     * @param approverRole 当前管理员审批角色
     * @return 待审批续假申请列表
     */
    private List<Map<String, Object>> queryPendingExtensions(String like, String approverRole) {
        return jdbcTemplate.queryForList(
                "SELECT l.id, er.id AS extension_id, 'extension' AS approval_kind, l.student_id, l.student_name, " +
                        "CONCAT('续假-', l.leave_type) AS leave_type, er.reason, er.old_return_time AS start_time, " +
                        "er.new_return_time AS end_time, er.approval_level, er.approval_level AS total_steps, er.created_at, " +
                        "er.current_step AS step_no, " +
                        "CASE er.current_step WHEN 1 THEN '审批人(辅导员)' WHEN 2 THEN '审批人(学院党委副书记)' ELSE '审批人(学院分管副院长)' END AS approver_role, " +
                        "CASE er.current_step WHEN 1 THEN 'LIHUANYU' WHEN 2 THEN 'WANGJIE' ELSE 'ZHANGQIANG' END AS approver_name, " +
                        "er.status AS step_status " +
                        "FROM return_extension_requests er JOIN leave_applications l ON l.id = er.leave_id " +
                        "WHERE er.status = 'pending' AND " +
                        "CASE er.current_step WHEN 1 THEN '审批人(辅导员)' WHEN 2 THEN '审批人(学院党委副书记)' ELSE '审批人(学院分管副院长)' END = ? " +
                        "AND (l.student_name LIKE ? OR l.student_id LIKE ?) ORDER BY er.created_at ASC",
                approverRole, like, like);
    }

    /**
     * 查询管理员审批统计数（待审批请假、待审批返校、超时未返校）
     * @return 统计数据Map
     */
    private Map<String, Object> getAdminCounts() {
        String approverRole = roleToApproverRole(getAdminRole());
        String bareRole = bareApproverRole(approverRole);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT ((SELECT COUNT(*) FROM leave_applications l JOIN approval_steps s ON s.leave_id = l.id AND s.status = 'pending' " +
                        "WHERE l.status = 'pending' AND s.approver_role IN (?, ?)) + " +
                        "(SELECT COUNT(*) FROM return_extension_requests er WHERE er.status = 'pending' AND " +
                        "CASE er.current_step WHEN 1 THEN '审批人(辅导员)' WHEN 2 THEN '审批人(学院党委副书记)' ELSE '审批人(学院分管副院长)' END = ?)) AS leave_pending, " +
                        "(SELECT COUNT(*) FROM return_requests WHERE status = 'pending') AS return_pending, " +
                        "(SELECT COUNT(*) FROM leave_applications l WHERE l.status = 'approved' AND l.end_time < NOW()) AS overtime_count",
                approverRole, bareRole, approverRole);

        // 封装统计数据
        Map<String, Object> counts = new HashMap<String, Object>();
        counts.put("leavePending", row.get("leave_pending"));
        counts.put("returnPending", row.get("return_pending"));
        counts.put("overtimeCount", row.get("overtime_count"));
        return counts;
    }

    /**
     * 获取当前管理员角色（从数据库配置中读取）
     * @return 当前角色名称
     */
    private String getAdminRole() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT current_role FROM admin_role_settings WHERE id = 1 LIMIT 1");
        return rows.isEmpty() ? "辅导员" : String.valueOf(rows.get(0).get("current_role"));
    }

    /**
     * 查询审批历史数据
     * @param scope 历史范围（mine-我审批的/cc-抄送我的/all-全部）
     * @param search 搜索关键词（学号/姓名）
     * @return 审批历史列表
     */
    private List<Map<String, Object>> queryHistory(String scope, String search) {
        String like = "%" + (search == null ? "" : search) + "%";
        String currentRole = getAdminRole();
        String approverRole = roleToApproverRole(currentRole);
        String bareRole = bareApproverRole(approverRole);

        // 我审批的历史
        if ("mine".equals(scope)) {
            return jdbcTemplate.queryForList(
                    "SELECT DISTINCT l.id, l.student_id, l.student_name, l.leave_type, l.reason, l.start_time, l.end_time, " +
                            "l.created_at, s.status AS record_status, s.decided_at AS record_time " +
                            "FROM approval_steps s JOIN leave_applications l ON l.id = s.leave_id " +
                            "WHERE s.approver_role IN (?, ?) AND s.status IN ('approved','rejected') " +
                            "AND (l.student_name LIKE ? OR l.student_id LIKE ?) ORDER BY s.decided_at DESC, l.created_at DESC",
                    approverRole, bareRole, like, like);
        }

        // 抄送我的历史
        if ("cc".equals(scope)) {
            return jdbcTemplate.queryForList(
                    "SELECT l.id, l.student_id, l.student_name, l.leave_type, l.reason, l.start_time, l.end_time, " +
                            "l.created_at, 'cc' AS record_status, c.created_at AS record_time " +
                            "FROM admin_cc_records c JOIN leave_applications l ON l.id = c.leave_id " +
                            "WHERE c.receiver_role = ? AND (l.student_name LIKE ? OR l.student_id LIKE ?) ORDER BY c.created_at DESC",
                    currentRole, like, like);
        }

        // 全部历史
        return jdbcTemplate.queryForList(
                "SELECT l.id, l.student_id, l.student_name, l.leave_type, l.reason, l.start_time, l.end_time, l.created_at, " +
                        "l.status AS record_status, l.updated_at AS record_time " +
                        "FROM leave_applications l WHERE (l.student_name LIKE ? OR l.student_id LIKE ?) AND l.status <> 'pending' " +
                        "ORDER BY l.updated_at DESC, l.created_at DESC",
                like, like);
    }

    /**
     * 查询请假统计数据
     * @return 统计数据（省份分布、请假类型、学院统计、留校/离校占比）
     */
    private Map<String, Object> queryStatistics() {
        // 省份分布统计
        List<Map<String, Object>> provinces = jdbcTemplate.queryForList(
                "SELECT COALESCE(JSON_UNQUOTE(JSON_EXTRACT(address_levels, '$[0]')), '未录入省份') AS province, COUNT(*) AS student_count " +
                        "FROM leave_applications WHERE status IN ('pending','approved') GROUP BY province ORDER BY student_count DESC");
        // 请假类型统计
        List<Map<String, Object>> types = jdbcTemplate.queryForList(
                "SELECT leave_type, COUNT(*) AS count FROM leave_applications GROUP BY leave_type ORDER BY count DESC, leave_type ASC");
        // 学院统计（请假数、返校数）
        List<Map<String, Object>> colleges = jdbcTemplate.queryForList(
                "SELECT COALESCE(sp.college, '未录入学院') AS college, COUNT(l.id) AS leave_count, " +
                        "SUM(rr.id IS NOT NULL) AS return_count FROM leave_applications l " +
                        "LEFT JOIN student_profiles sp ON sp.student_id = l.student_id " +
                        "LEFT JOIN return_requests rr ON rr.leave_id = l.id GROUP BY COALESCE(sp.college, '未录入学院') ORDER BY leave_count DESC");
        // 留校/离校占比
        Map<String, Object> ratio = jdbcTemplate.queryForMap(
                "SELECT SUM(is_outside_jinzhong = 0) AS stayCount, SUM(is_outside_jinzhong = 1) AS leaveCount FROM leave_applications");

        // 封装统计数据
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("provinces", provinces);
        data.put("types", types);
        data.put("ratio", ratio);
        data.put("colleges", colleges);
        return data;
    }

    /**
     * 请假审批通过流程处理
     * @param leaveId 请假单ID
     * @return 审批结果
     * @throws IllegalArgumentException 申请不存在或已处理时抛出
     */
    private Map<String, Object> approveLeaveFlow(Long leaveId) {
        // 查询当前待审批节点
        Map<String, Object> step = singleRow(
                "SELECT step_no, approver_role FROM approval_steps WHERE leave_id = ? AND status = 'pending' ORDER BY step_no LIMIT 1",
                leaveId);
        // 校验当前管理员是否有权审批该节点
        assertRole(step.get("approver_role"), "当前角色无权审批该节点");
        int stepNo = toInt(step.get("step_no"), 1);
        int nextStepNo = stepNo + 1;
        // 判断是否有下一审批节点
        boolean hasNext = !jdbcTemplate.queryForList(
                "SELECT step_no FROM approval_steps WHERE leave_id = ? AND step_no = ? LIMIT 1",
                leaveId, nextStepNo).isEmpty();

        // 更新当前节点为已通过
        jdbcTemplate.update("UPDATE approval_steps SET status = 'approved', comment = '已同意', decided_at = NOW() WHERE leave_id = ? AND step_no = ?",
                leaveId, stepNo);
        // 有下一节点：更新下一节点为待审批
        if (hasNext) {
            jdbcTemplate.update("UPDATE approval_steps SET status = 'pending' WHERE leave_id = ? AND step_no = ?", leaveId, nextStepNo);
        } else {
            // 无下一节点：更新请假单为已通过
            jdbcTemplate.update("UPDATE leave_applications SET status = 'approved' WHERE id = ?", leaveId);
        }
        // 返回审批结果
        return approvalResult(leaveId, hasNext ? "pending" : "approved", hasNext ? "已同意并上报下一审批人" : "请假审批通过");
    }

    /**
     * 请假审批拒绝流程处理
     * @param leaveId 请假单ID
     * @param comment 拒绝理由
     * @return 审批结果
     * @throws IllegalArgumentException 申请不存在或已处理时抛出
     */
    private Map<String, Object> rejectLeaveFlow(Long leaveId, String comment) {
        // 查询当前待审批节点
        Map<String, Object> step = singleRow(
                "SELECT step_no, approver_role FROM approval_steps WHERE leave_id = ? AND status = 'pending' ORDER BY step_no LIMIT 1",
                leaveId);
        // 校验当前管理员是否有权审批该节点
        assertRole(step.get("approver_role"), "当前角色无权审批该节点");
        int stepNo = toInt(step.get("step_no"), 1);
        // 更新当前节点为已拒绝，记录拒绝理由
        jdbcTemplate.update("UPDATE approval_steps SET status = 'rejected', comment = ?, decided_at = NOW() WHERE leave_id = ? AND step_no = ?",
                StringUtils.hasText(comment) ? comment : "不同意请假", leaveId, stepNo);
        // 更新其他等待节点为已拒绝
        jdbcTemplate.update("UPDATE approval_steps SET status = 'rejected', comment = '上级节点已拒绝', decided_at = NOW() WHERE leave_id = ? AND status = 'waiting'",
                leaveId);
        // 更新请假单为已拒绝
        jdbcTemplate.update("UPDATE leave_applications SET status = 'rejected' WHERE id = ?", leaveId);
        // 返回审批结果
        return approvalResult(leaveId, "rejected", "已拒绝请假");
    }

    /**
     * 续假审批通过流程处理
     * @param extensionId 续假申请ID
     * @return 审批结果
     * @throws IllegalArgumentException 申请不存在或已处理时抛出
     */
    private Map<String, Object> approveExtensionFlow(Long extensionId) {
        // 查询续假申请信息
        Map<String, Object> request = singleRow(
                "SELECT id, leave_id, new_return_time, approval_level, current_step FROM return_extension_requests WHERE id = ? AND status = 'pending' LIMIT 1",
                extensionId);
        int currentStep = toInt(request.get("current_step"), 1);
        int approvalLevel = toInt(request.get("approval_level"), 1);
        // 校验当前管理员是否有权审批该节点
        assertRole(approvalStepByNo(currentStep).get("approver_role"), "当前角色无权审批该续假节点");
        // 判断是否有下一审批节点
        boolean hasNext = currentStep < approvalLevel;

        // 有下一节点：更新当前步骤为下一步
        if (hasNext) {
            jdbcTemplate.update("UPDATE return_extension_requests SET current_step = ?, comment = '已同意续假并上报' WHERE id = ?",
                    currentStep + 1, extensionId);
        } else {
            // 无下一节点：更新续假申请为已通过，并更新请假单结束时间
            jdbcTemplate.update("UPDATE return_extension_requests SET status = 'approved', comment = '已同意续假' WHERE id = ?", extensionId);
            jdbcTemplate.update(
                    "UPDATE leave_applications SET end_time = ?, duration_minutes = TIMESTAMPDIFF(MINUTE, start_time, ?) WHERE id = ?",
                    request.get("new_return_time"), request.get("new_return_time"), request.get("leave_id"));
        }
        // 返回审批结果
        return approvalResult(extensionId, hasNext ? "pending" : "approved", hasNext ? "已同意续假并上报下一审批人" : "续假审批通过");
    }

    /**
     * 续假审批拒绝流程处理
     * @param extensionId 续假申请ID
     * @param comment 拒绝理由
     * @return 审批结果
     * @throws IllegalArgumentException 申请不存在或已处理时抛出
     */
    private Map<String, Object> rejectExtensionFlow(Long extensionId, String comment) {
        // 查询续假申请信息
        Map<String, Object> request = singleRow(
                "SELECT id, current_step FROM return_extension_requests WHERE id = ? AND status = 'pending' LIMIT 1",
                extensionId);
        int currentStep = toInt(request.get("current_step"), 1);
        // 校验当前管理员是否有权审批该节点
        assertRole(approvalStepByNo(currentStep).get("approver_role"), "当前角色无权审批该续假节点");
        // 更新续假申请为已拒绝，记录拒绝理由
        jdbcTemplate.update("UPDATE return_extension_requests SET status = 'rejected', comment = ? WHERE id = ?",
                StringUtils.hasText(comment) ? comment : "不同意续假", extensionId);
        // 返回审批结果
        return approvalResult(extensionId, "rejected", "已拒绝续假");
    }

    /**
     * 查询请假单最新的待审批续假申请
     * @param leaveId 请假单ID
     * @return 续假申请信息（无则返回null）
     */
    private Map<String, Object> latestExtensionByLeave(Long leaveId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM return_extension_requests WHERE leave_id = ? AND status = 'pending' ORDER BY created_at DESC LIMIT 1",
                leaveId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 执行SQL查询并返回单行数据
     * @param sql SQL语句
     * @param args SQL参数
     * @return 单行数据Map
     * @throws IllegalArgumentException 无数据时抛出异常
     */
    private Map<String, Object> singleRow(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("当前申请不存在或已处理");
        }
        return rows.get(0);
    }

    /**
     * 校验当前管理员角色是否有权审批指定节点
     * @param expectedRole 节点所需角色
     * @param message 校验失败提示信息
     * @throws IllegalStateException 角色不匹配时抛出异常
     */
    private void assertRole(Object expectedRole, String message) {
        String currentRole = roleToApproverRole(getAdminRole());
        String expected = String.valueOf(expectedRole);
        if (!currentRole.equals(expected) && !bareApproverRole(currentRole).equals(expected)) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 将管理员角色转换为审批角色（如：辅导员 → 审批人(辅导员)）
     * @param role 原始角色
     * @return 审批角色
     */
    private String roleToApproverRole(String role) {
        if ("学院分管副院长".equals(role)) return "审批人(学院分管副院长)";
        if ("学院党委副书记".equals(role)) return "审批人(学院党委副书记)";
        return "审批人(辅导员)";
    }

    /**
     * 将审批角色还原为原始角色（如：审批人(辅导员) → 辅导员）
     * @param approverRole 审批角色
     * @return 原始角色
     */
    private String bareApproverRole(String approverRole) {
        if ("审批人(学院分管副院长)".equals(approverRole)) return "学院分管副院长";
        if ("审批人(学院党委副书记)".equals(approverRole)) return "学院党委副书记";
        return "辅导员";
    }

    /**
     * 根据步骤号获取审批节点角色
     * @param stepNo 步骤号（1-辅导员，2-党委副书记，3-分管副院长）
     * @return 节点角色信息
     */
    private Map<String, Object> approvalStepByNo(int stepNo) {
        Map<String, Object> step = new HashMap<String, Object>();
        if (stepNo == 1) {
            step.put("approver_role", "审批人(辅导员)");
            return step;
        }
        if (stepNo == 2) {
            step.put("approver_role", "审批人(学院党委副书记)");
            return step;
        }
        step.put("approver_role", "审批人(学院分管副院长)");
        return step;
    }

    /**
     * 构建审批结果返回数据
     * @param id 申请ID（请假/续假/返校）
     * @param status 审批状态（pending/approved/rejected）
     * @param message 审批提示信息
     * @return 结果Map
     */
    private Map<String, Object> approvalResult(Object id, String status, String message) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", id);
        data.put("status", status);
        data.put("message", message);
        return data;
    }

    /**
     * 转换对象为整数（失败返回默认值）
     * @param value 待转换对象
     * @param fallback 默认值
     * @return 转换后的整数
     */
    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * 转换对象为长整数
     * @param value 待转换对象
     * @return 转换后的长整数
     */
    private Long toLong(Object value) {
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * 审批请求体封装类（包含审批意见/拒绝理由）
     */
    @Data
    public static class ApprovalRequest {
        // 审批意见/拒绝理由
        private String comment;
    }

    /**
     * 角色修改请求体封装类
     */
    @Data
    public static class RoleRequest {
        // 目标角色
        private String role;
    }
}