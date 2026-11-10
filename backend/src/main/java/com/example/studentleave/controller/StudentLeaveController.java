package com.example.studentleave.controller;

import com.example.studentleave.common.Result;
import com.example.studentleave.entity.LeaveApplication;
import com.example.studentleave.entity.ReturnExtensionRequest;
import com.example.studentleave.entity.ReturnRequest;
import com.example.studentleave.entity.StudentAccount;
import com.example.studentleave.entity.StudentProfile;
import com.example.studentleave.mapper.ApprovalStepMapper;
import com.example.studentleave.mapper.LeaveApplicationMapper;
import com.example.studentleave.mapper.ReturnExtensionRequestMapper;
import com.example.studentleave.mapper.ReturnRequestMapper;
import com.example.studentleave.mapper.StudentAccountMapper;
import com.example.studentleave.mapper.StudentProfileMapper;
import com.example.studentleave.service.LeaveApplicationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生请假管理控制器
 * 处理学生请假、销假、续假相关的HTTP请求，包括请假申请创建、撤销、记录查询、详情查询等功能
 */
@RestController
@RequestMapping("/api")
public class StudentLeaveController {

    // 依赖注入 - 请假申请业务层
    private final LeaveApplicationService leaveApplicationService;
    // 依赖注入 - 请假申请数据访问层
    private final LeaveApplicationMapper leaveApplicationMapper;
    // 依赖注入 - 学生档案数据访问层
    private final StudentProfileMapper studentProfileMapper;
    // 依赖注入 - 学生账户数据访问层
    private final StudentAccountMapper studentAccountMapper;
    // 依赖注入 - 销假申请数据访问层
    private final ReturnRequestMapper returnRequestMapper;
    // 依赖注入 - 续假申请数据访问层
    private final ReturnExtensionRequestMapper returnExtensionRequestMapper;
    // 依赖注入 - 审批步骤数据访问层
    private final ApprovalStepMapper approvalStepMapper;
    // 依赖注入 - JSON序列化/反序列化工具
    private final ObjectMapper objectMapper;
    // 依赖注入 - JDBC模板，用于自定义SQL查询
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数注入所有依赖组件
     * @param leaveApplicationService 请假申请业务服务
     * @param leaveApplicationMapper 请假申请Mapper
     * @param studentProfileMapper 学生档案Mapper
     * @param studentAccountMapper 学生账户Mapper
     * @param returnRequestMapper 销假申请Mapper
     * @param returnExtensionRequestMapper 续假申请Mapper
     * @param approvalStepMapper 审批步骤Mapper
     * @param objectMapper JSON处理工具
     * @param jdbcTemplate JDBC模板
     */
    public StudentLeaveController(LeaveApplicationService leaveApplicationService,
                                  LeaveApplicationMapper leaveApplicationMapper,
                                  StudentProfileMapper studentProfileMapper,
                                  StudentAccountMapper studentAccountMapper,
                                  ReturnRequestMapper returnRequestMapper,
                                  ReturnExtensionRequestMapper returnExtensionRequestMapper,
                                  ApprovalStepMapper approvalStepMapper,
                                  ObjectMapper objectMapper,
                                  JdbcTemplate jdbcTemplate) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveApplicationMapper = leaveApplicationMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.studentAccountMapper = studentAccountMapper;
        this.returnRequestMapper = returnRequestMapper;
        this.returnExtensionRequestMapper = returnExtensionRequestMapper;
        this.approvalStepMapper = approvalStepMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取学生首页数据（包含学生信息和有效请假申请）
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含学生基础信息和有效请假数据
     */
    @GetMapping({"/student-home", "/active-leave"})
    public Result<Map<String, Object>> getHome(HttpSession session) {
        try {
            // 获取当前登录学生账户信息
            StudentAccount account = currentAccount(session, null);
            Map<String, Object> data = new HashMap<String, Object>();
            // 构建学生基础数据
            data.put("student", buildStudentData(account));
            // 若学生无需强制改密码，则查询其有效请假数据；否则返回null
            data.put("active", Boolean.TRUE.equals(account.getMustChangePassword()) ? null : queryStudentHome(account.getStudentId()));
            return Result.success(data);
        } catch (Exception e) {
            // 异常时返回401未授权，携带异常信息
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 查询学生的请假记录
     * @param period 时间范围筛选（all：全部，其他：近一个月）
     * @param studentId 学生ID（可选，优先从会话获取）
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含学生请假记录列表
     */
    @GetMapping("/leave-records")
    public Result<Map<String, Object>> listMyLeaves(@RequestParam(required = false) String period,
                                                    @RequestParam(required = false) String studentId,
                                                    HttpSession session) {
        try {
            // 获取当前登录学生账户信息
            StudentAccount account = currentAccount(session, studentId);
            // 首次登录需改密码时，拒绝查询
            if (Boolean.TRUE.equals(account.getMustChangePassword())) {
                return Result.error(403, "首次登录请先修改密码");
            }

            Map<String, Object> data = new HashMap<String, Object>();
            // 查询指定学生的请假记录（按时间范围筛选）
            data.put("records", queryLeaveRecords(account.getStudentId(), period));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 查询请假申请详情
     * @param id 请假申请ID
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含请假申请详情、审批步骤、统计数据等
     */
    @GetMapping("/leaves/{id}")
    public Result<Map<String, Object>> getLeaveDetail(@PathVariable Long id, HttpSession session) {
        try {
            // 获取当前登录学生账户信息
            StudentAccount account = currentAccount(session, null);
            // 首次登录需改密码时，拒绝查询
            if (Boolean.TRUE.equals(account.getMustChangePassword())) {
                return Result.error(403, "首次登录请先修改密码");
            }
            // 查询请假详情（仅允许查询本人申请）
            return Result.success(queryLeaveDetail(id, true, account.getStudentId()));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 创建新的请假申请
     * @param request 请假申请请求参数（包含请假类型、时间、地址、联系方式等）
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含创建成功的请假申请信息
     */
    @PostMapping("/leaves")
    public Result<LeaveApplication> createLeave(@RequestBody CreateLeaveRequest request, HttpSession session) {
        try {
            // 校验并获取当前学生ID
            String studentId = currentAccount(session, request.getStudentId()).getStudentId();
            // 查询学生档案信息
            StudentProfile profile = studentProfileMapper.selectById(studentId);
            if (profile == null) {
                return Result.error(404, "学生信息不存在");
            }

            // 构建请假申请实体
            LeaveApplication leave = new LeaveApplication();
            leave.setStudentId(studentId);
            leave.setStudentName(profile.getStudentName());
            leave.setIsOutsideJinzhong(request.getIsOutsideJinzhong()); // 是否出晋中
            leave.setLeaveType(request.getLeaveType()); // 请假类型
            leave.setAddressLevels(toJson(request.getAddressLevels())); // 地址层级（JSON序列化）
            leave.setDetailedAddress(request.getDetailedAddress()); // 详细地址
            leave.setStartTime(request.getStartTime()); // 请假开始时间
            leave.setEndTime(request.getEndTime()); // 请假结束时间
            leave.setReason(request.getReason()); // 请假原因
            leave.setAttachmentsJson(toJson(request.getAttachments())); // 附件（JSON序列化）
            leave.setStudentPhone(request.getStudentPhone()); // 学生联系电话
            leave.setEmergencyContactName(request.getEmergencyContactName()); // 紧急联系人姓名
            leave.setEmergencyContactPhone(request.getEmergencyContactPhone()); // 紧急联系人电话

            // 调用业务层创建请假申请
            return Result.success("提交成功", leaveApplicationService.createLeave(leave));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 撤销请假申请（仅审批中状态可撤销）
     * @param id 请假申请ID
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含撤销后的请假状态
     */
    @PostMapping("/leaves/{id}/cancel")
    @Transactional(rollbackFor = Exception.class) // 事务注解，异常时回滚
    public Result<Map<String, Object>> cancelLeave(@PathVariable Long id, HttpSession session) {
        try {
            // 获取当前登录学生账户信息
            StudentAccount account = currentAccount(session, null);
            // 查询请假申请
            LeaveApplication leave = leaveApplicationMapper.selectById(id);
            // 校验请假申请是否存在且属于当前学生
            if (leave == null || !account.getStudentId().equals(leave.getStudentId())) {
                return Result.error(404, "请假申请不存在");
            }
            // 仅审批中（pending）的请假可撤销
            if (!"pending".equals(leave.getStatus())) {
                return Result.error(400, "只有审批进行中的请假可以撤销");
            }

            // 更新请假状态为已撤销
            leave.setStatus("cancelled");
            leaveApplicationMapper.updateById(leave);
            // 更新审批步骤状态为待处理（等待）
            jdbcTemplate.update("UPDATE approval_steps SET status = 'waiting' WHERE leave_id = ? AND status = 'pending'", id);

            Map<String, Object> data = new HashMap<String, Object>();
            data.put("id", id);
            data.put("status", "cancelled");
            return Result.success("撤销成功", data);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 提交销假申请（仅已通过的假单可发起）
     * @param request 销假申请请求参数（包含假单ID、销假时间、备注等）
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含创建成功的销假申请信息
     */
    @PostMapping("/return-requests")
    public Result<ReturnRequest> createReturnRequest(@RequestBody CreateReturnRequest request, HttpSession session) {
        try {
            // 校验并获取当前学生ID
            String studentId = currentAccount(session, request.getStudentId()).getStudentId();
            // 查询对应的请假申请（校验归属）
            LeaveApplication leave = getStudentLeave(request.getLeaveId(), studentId);
            // 仅已通过（approved）的假单可销假
            if (!"approved".equals(leave.getStatus())) {
                return Result.error(400, "只有已通过的假单可以发起销假");
            }

            // 构建销假申请实体
            ReturnRequest returnRequest = new ReturnRequest();
            returnRequest.setLeaveId(leave.getId()); // 关联的请假ID
            returnRequest.setStudentId(studentId); // 学生ID
            returnRequest.setReturnTime(request.getReturnTime()); // 销假时间
            returnRequest.setNote(request.getNote()); // 销假备注
            returnRequest.setStatus("pending"); // 初始状态：审批中
            // 插入销假申请记录
            returnRequestMapper.insert(returnRequest);
            return Result.success("返校销假申请提交成功", returnRequest);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 提交续假申请（仅审批中/已通过的假单可发起）
     * @param request 续假申请请求参数（包含假单ID、新返校时间、续假原因等）
     * @param session HTTP会话，用于获取当前登录学生信息
     * @return 封装后的响应结果，包含创建成功的续假申请信息
     */
    @PostMapping("/extension-requests")
    public Result<ReturnExtensionRequest> createExtensionRequest(@RequestBody CreateExtensionRequest request,
                                                                 HttpSession session) {
        try {
            // 校验并获取当前学生ID
            String studentId = currentAccount(session, request.getStudentId()).getStudentId();
            // 查询对应的请假申请（校验归属）
            LeaveApplication leave = getStudentLeave(request.getLeaveId(), studentId);
            // 校验假单状态：仅审批中（pending）/已通过（approved）可续假
            if (!"approved".equals(leave.getStatus()) && !"pending".equals(leave.getStatus())) {
                return Result.error(400, "当前假单状态不能发起续假");
            }
            // 校验新返校时间：必须晚于原结束时间
            if (request.getNewReturnTime() == null || !request.getNewReturnTime().isAfter(leave.getEndTime())) {
                return Result.error(400, "新的返校时间必须晚于原结束时间");
            }

            // 构建续假申请实体
            ReturnExtensionRequest extensionRequest = new ReturnExtensionRequest();
            extensionRequest.setLeaveId(leave.getId()); // 关联的请假ID
            extensionRequest.setStudentId(studentId); // 学生ID
            extensionRequest.setOldReturnTime(leave.getEndTime()); // 原返校时间
            extensionRequest.setNewReturnTime(request.getNewReturnTime()); // 新返校时间
            extensionRequest.setReason(request.getReason()); // 续假原因
            // 计算续假审批层级（根据续假天数）
            extensionRequest.setApprovalLevel(calculateApprovalLevel(leave.getEndTime(), request.getNewReturnTime()));
            extensionRequest.setCurrentStep(1); // 初始审批步骤：第1步
            extensionRequest.setStatus("pending"); // 初始状态：审批中
            // 插入续假申请记录
            returnExtensionRequestMapper.insert(extensionRequest);

            return Result.success("续假申请提交成功", extensionRequest);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 查询学生首页的有效请假数据（审批中/已通过的最新一条请假）
     * @param studentId 学生ID
     * @return 包含请假、销假、续假信息的Map，无数据则返回null
     */
    private Map<String, Object> queryStudentHome(String studentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT l.id, NULL AS extension_id, 'leave' AS approval_kind, l.student_id, l.student_name, " +
                        "l.leave_type, l.start_time, l.end_time, l.status AS leave_status, l.created_at, " +
                        "rr.id AS return_id, rr.status AS return_status, rr.return_time, " +
                        "er.id AS extension_id, er.status AS extension_status, er.old_return_time, er.new_return_time, " +
                        "er.reason AS extension_reason, er.created_at AS extension_created_at, " +
                        "er.approval_level AS extension_approval_level, er.current_step AS extension_current_step " +
                        "FROM leave_applications l " +
                        "LEFT JOIN return_requests rr ON rr.id = (SELECT id FROM return_requests WHERE leave_id = l.id ORDER BY created_at DESC LIMIT 1) " +
                        "LEFT JOIN return_extension_requests er ON er.id = (SELECT id FROM return_extension_requests WHERE leave_id = l.id ORDER BY created_at DESC LIMIT 1) " +
                        "WHERE l.student_id = ? AND l.status IN ('pending','approved') ORDER BY l.created_at DESC LIMIT 1",
                studentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 查询学生的请假记录列表（支持时间范围筛选）
     * @param studentId 学生ID
     * @param period 时间范围（all：全部，其他：近一个月）
     * @return 请假记录列表
     */
    private List<Map<String, Object>> queryLeaveRecords(String studentId, String period) {
        // 构建时间筛选条件
        String timeFilter = "all".equals(period) ? "" : " AND l.created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH) ";
        return jdbcTemplate.queryForList(
                "SELECT l.id, NULL AS extension_id, 'leave' AS approval_kind, l.student_id, l.student_name, " +
                        "l.leave_type, l.reason, l.start_time, l.end_time, l.status, l.created_at, " +
                        "rr.status AS return_status, rr.return_time " +
                        "FROM leave_applications l " +
                        "LEFT JOIN return_requests rr ON rr.id = (SELECT id FROM return_requests WHERE leave_id = l.id ORDER BY created_at DESC LIMIT 1) " +
                        "WHERE l.student_id = ? " + timeFilter +
                        "ORDER BY l.created_at DESC",
                studentId);
    }

    /**
     * 查询请假申请详情（包含审批步骤、学生档案、销假/续假信息）
     * @param id 请假申请ID
     * @param studentOnly 是否仅允许查询本人申请
     * @param studentId 学生ID（studentOnly=true时必填）
     * @return 请假详情Map（包含请假信息、审批步骤、统计数据）
     * @throws JsonProcessingException JSON反序列化异常
     */
    public Map<String, Object> queryLeaveDetail(Long id, boolean studentOnly, String studentId) throws JsonProcessingException {
        List<Object> args = new ArrayList<Object>();
        args.add(id);
        String studentWhere = "";
        // 若仅允许查询本人申请，添加学生ID筛选条件
        if (studentOnly) {
            studentWhere = " AND l.student_id = ? ";
            args.add(studentId);
        }

        // 查询请假基础信息、销假信息、续假信息、学生档案
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT l.*, rr.status AS return_status, rr.id AS return_id, rr.return_time, rr.note AS return_note, " +
                        "rr.comment AS return_comment, er.id AS extension_id, er.status AS extension_status, " +
                        "er.old_return_time, er.new_return_time, er.reason AS extension_reason, er.comment AS extension_comment, " +
                        "er.created_at AS extension_created_at, er.approval_level AS extension_approval_level, er.current_step AS extension_current_step, " +
                        "sp.college, sp.grade, sp.major, sp.class_name " +
                        "FROM leave_applications l " +
                        "LEFT JOIN return_requests rr ON rr.id = (SELECT id FROM return_requests WHERE leave_id = l.id ORDER BY created_at DESC LIMIT 1) " +
                        "LEFT JOIN return_extension_requests er ON er.id = (SELECT id FROM return_extension_requests WHERE leave_id = l.id ORDER BY created_at DESC LIMIT 1) " +
                        "LEFT JOIN student_profiles sp ON sp.student_id = l.student_id " +
                        "WHERE l.id = ? " + studentWhere + " LIMIT 1",
                args.toArray());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("请假申请不存在");
        }

        Map<String, Object> leave = rows.get(0);
        // 反序列化地址层级（JSON转List）
        leave.put("address_levels", parseJsonList(leave.get("address_levels")));
        // 反序列化附件（JSON转List）
        leave.put("attachments", parseJsonList(leave.get("attachments_json")));
        leave.remove("attachments_json"); // 移除原始JSON字段

        // 查询审批步骤详情
        List<Map<String, Object>> steps = queryDetailSteps(id, leave);
        // 查询该学生本月/本年请假次数
        Map<String, Object> countRow = jdbcTemplate.queryForMap(
                "SELECT SUM(created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')) AS month_count, " +
                        "SUM(created_at >= MAKEDATE(YEAR(NOW()), 1)) AS year_count " +
                        "FROM leave_applications WHERE student_id = ?",
                leave.get("student_id"));

        // 组装最终返回数据
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("leave", leave); // 请假基础信息
        data.put("steps", steps); // 审批步骤
        data.put("monthCount", countRow.get("month_count") == null ? 0 : countRow.get("month_count")); // 本月请假次数
        data.put("yearCount", countRow.get("year_count") == null ? 0 : countRow.get("year_count")); // 本年请假次数
        return data;
    }

    /**
     * 查询请假/续假的审批步骤详情
     * @param leaveId 请假申请ID
     * @param leave 请假基础信息Map
     * @return 审批步骤列表
     */
    private List<Map<String, Object>> queryDetailSteps(Long leaveId, Map<String, Object> leave) {
        Object extensionStatus = leave.get("extension_status");
        Object returnStatus = leave.get("return_status");
        // 若存在续假申请且无销假申请，返回续假审批步骤
        if (extensionStatus != null && returnStatus == null) {
            int level = toInt(leave.get("extension_approval_level"), 1); // 续假审批层级
            int currentStep = toInt(leave.get("extension_current_step"), 1); // 当前审批步骤
            List<Map<String, Object>> steps = new ArrayList<Map<String, Object>>();
            // 构建各层级审批步骤
            for (int i = 1; i <= level; i++) {
                Map<String, Object> row = approvalStepByNo(i); // 根据步骤号获取审批人信息
                row.put("step_no", i);
                // 设置步骤状态：审批中则区分已通过/审批中/待处理；否则使用续假最终状态
                row.put("status", "pending".equals(String.valueOf(extensionStatus))
                        ? (i < currentStep ? "approved" : i == currentStep ? "pending" : "waiting")
                        : extensionStatus);
                // 设置审批备注
                row.put("comment", "approved".equals(String.valueOf(extensionStatus)) || i < currentStep ? "已同意续假" : leave.get("extension_comment"));
                row.put("created_at", leave.get("extension_created_at")); // 续假申请创建时间
                steps.add(row);
            }
            return steps;
        }

        // 无续假或有销假时，查询原始审批步骤
        return jdbcTemplate.queryForList(
                "SELECT step_no, approver_role, approver_name, approver_code, status, comment, decided_at, created_at " +
                        "FROM approval_steps WHERE leave_id = ? ORDER BY step_no",
                leaveId);
    }

    /**
     * 获取当前登录学生的账户信息（会话优先，其次使用传入的studentId）
     * @param session HTTP会话
     * @param fallbackStudentId 备用学生ID（会话无信息时使用）
     * @return 学生账户信息
     * @throws IllegalStateException 未登录/登录失效时抛出异常
     */
    private StudentAccount currentAccount(HttpSession session, String fallbackStudentId) {
        Object sessionStudentId = session.getAttribute("studentId");
        String studentId = sessionStudentId == null ? null : String.valueOf(sessionStudentId);
        // 校验学生ID是否有效
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalStateException("请先登录");
        }
        // 查询学生账户信息
        StudentAccount account = studentAccountMapper.selectById(studentId);
        if (account == null) {
            throw new IllegalStateException("登录状态已失效，请重新登录");
        }
        return account;
    }

    /**
     * 构建学生基础数据（用于前端展示）
     * @param account 学生账户信息
     * @return 学生基础信息Map
     */
    private Map<String, Object> buildStudentData(StudentAccount account) {
        StudentProfile profile = studentProfileMapper.selectById(account.getStudentId());
        Map<String, Object> student = new HashMap<String, Object>();
        student.put("id", account.getStudentId());
        student.put("studentId", account.getStudentId());
        student.put("mustChangePassword", Boolean.TRUE.equals(account.getMustChangePassword())); // 是否需强制改密码
        // 若存在学生档案，补充档案信息
        if (profile != null) {
            student.put("name", profile.getStudentName());
            student.put("studentName", profile.getStudentName());
            student.put("gender", profile.getGender());
            student.put("college", profile.getCollege()); // 学院
            student.put("grade", profile.getGrade()); // 年级
            student.put("major", profile.getMajor()); // 专业
            student.put("className", profile.getClassName()); // 班级
        }
        return student;
    }

    /**
     * 查询指定学生的请假申请（校验归属）
     * @param leaveId 请假申请ID
     * @param studentId 学生ID
     * @return 请假申请实体
     * @throws IllegalArgumentException 假单ID为空/假单不存在/假单不属于该学生时抛出异常
     */
    private LeaveApplication getStudentLeave(Long leaveId, String studentId) {
        if (leaveId == null) {
            throw new IllegalArgumentException("假单ID不能为空");
        }
        LeaveApplication leave = leaveApplicationMapper.selectById(leaveId);
        if (leave == null || !studentId.equals(leave.getStudentId())) {
            throw new IllegalArgumentException("假单不存在");
        }
        return leave;
    }

    /**
     * 将对象序列化为JSON字符串（null则返回空数组JSON）
     * @param value 待序列化对象
     * @return JSON字符串
     * @throws JsonProcessingException JSON序列化异常
     */
    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value == null ? Collections.emptyList() : value);
    }

    /**
     * 将JSON字符串反序列化为List（null/已为List则直接返回）
     * @param value 待反序列化的值（JSON字符串/List）
     * @return 反序列化后的List
     * @throws JsonProcessingException JSON反序列化异常
     */
    private Object parseJsonList(Object value) throws JsonProcessingException {
        if (value == null) return Collections.emptyList();
        if (value instanceof List) return value;
        return objectMapper.readValue(String.valueOf(value), List.class);
    }

    /**
     * 根据续假天数计算审批层级
     * @param oldReturnTime 原返校时间
     * @param newReturnTime 新返校时间
     * @return 审批层级（1：≤3天，2：≤10天，3：>10天）
     */
    private int calculateApprovalLevel(LocalDateTime oldReturnTime, LocalDateTime newReturnTime) {
        long minutes = Duration.between(oldReturnTime, newReturnTime).toMinutes(); // 续假分钟数
        int days = (int) ((minutes + 1439) / 1440); // 转换为天数（向上取整）
        if (days <= 3) return 1; // 3天内：辅导员审批
        if (days <= 10) return 2; // 3-10天：学院党委副书记审批
        return 3; // 10天以上：学院分管副院长审批
    }

    /**
     * 根据审批步骤号获取默认审批人信息
     * @param stepNo 审批步骤号（1/2/3）
     * @return 审批人信息Map（角色、姓名、编码）
     */
    private Map<String, Object> approvalStepByNo(int stepNo) {
        Map<String, Object> step = new HashMap<String, Object>();
        if (stepNo == 1) {
            step.put("approver_role", "审批人(辅导员)");
            step.put("approver_name", "LIHUANYU");
            step.put("approver_code", "HUANYU");
            return step;
        }
        if (stepNo == 2) {
            step.put("approver_role", "审批人(学院党委副书记)");
            step.put("approver_name", "WANGJIE");
            step.put("approver_code", "JIE");
            return step;
        }
        step.put("approver_role", "审批人(学院分管副院长)");
        step.put("approver_name", "ZHANGQIANG");
        step.put("approver_code", "QIANG");
        return step;
    }

    /**
     * 安全转换对象为整数（转换失败/值为null时返回默认值）
     * @param value 待转换值
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
     * 请假申请创建请求参数封装类
     * 接收前端提交的请假申请数据
     */
    @Data
    public static class CreateLeaveRequest {
        private String studentId; // 学生ID
        private Boolean isOutsideJinzhong; // 是否出晋中
        private String leaveType; // 请假类型
        private List<String> addressLevels; // 地址层级（省/市/区/街道）
        private String detailedAddress; // 详细地址
        private LocalDateTime startTime; // 请假开始时间
        private LocalDateTime endTime; // 请假结束时间
        private String reason; // 请假原因
        private Object attachments; // 附件（文件/图片等）
        private String studentPhone; // 学生联系电话
        private String emergencyContactName; // 紧急联系人姓名
        private String emergencyContactPhone; // 紧急联系人电话
    }

    /**
     * 销假申请创建请求参数封装类
     * 接收前端提交的销假申请数据
     */
    @Data
    public static class CreateReturnRequest {
        private Long leaveId; // 关联的请假申请ID
        private String studentId; // 学生ID
        private LocalDateTime returnTime; // 销假时间（返校时间）
        private String note; // 销假备注
    }

    /**
     * 续假申请创建请求参数封装类
     * 接收前端提交的续假申请数据
     */
    @Data
    public static class CreateExtensionRequest {
        private Long leaveId; // 关联的请假申请ID
        private String studentId; // 学生ID
        private LocalDateTime newReturnTime; // 新的返校时间（续假后结束时间）
        private String reason; // 续假原因
    }
}
