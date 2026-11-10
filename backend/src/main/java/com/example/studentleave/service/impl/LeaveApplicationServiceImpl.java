package com.example.studentleave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.studentleave.entity.ApprovalStep;
import com.example.studentleave.entity.LeaveApplication;
import com.example.studentleave.entity.ReturnRequest;
import com.example.studentleave.enums.LeaveStatusEnum;
import com.example.studentleave.mapper.ApprovalStepMapper;
import com.example.studentleave.mapper.LeaveApplicationMapper;
import com.example.studentleave.mapper.ReturnRequestMapper;
import com.example.studentleave.service.LeaveApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请业务接口实现类
 * 实现请假申请的创建、审批、销假确认等核心业务逻辑，包含完整的事务控制和参数校验
 *
 * @author 开发者名称
 * @date 开发日期
 */
@Service
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    // 审批动作：同意
    private static final String ACTION_APPROVE = "approve";
    // 审批动作：拒绝
    private static final String ACTION_REJECT = "reject";

    // 审批节点状态：待激活
    private static final String STEP_WAITING = "waiting";
    // 审批节点状态：待审批
    private static final String STEP_PENDING = "pending";
    // 审批节点状态：已同意
    private static final String STEP_APPROVED = "approved";
    // 审批节点状态：已拒绝
    private static final String STEP_REJECTED = "rejected";

    // 请假申请数据访问层
    private final LeaveApplicationMapper leaveApplicationMapper;
    // 审批节点数据访问层
    private final ApprovalStepMapper approvalStepMapper;
    // 销假申请数据访问层
    private final ReturnRequestMapper returnRequestMapper;

    /**
     * 构造函数注入依赖
     *
     * @param leaveApplicationMapper 请假申请Mapper
     * @param approvalStepMapper     审批节点Mapper
     * @param returnRequestMapper    销假申请Mapper
     */
    public LeaveApplicationServiceImpl(LeaveApplicationMapper leaveApplicationMapper,
                                       ApprovalStepMapper approvalStepMapper,
                                       ReturnRequestMapper returnRequestMapper) {
        this.leaveApplicationMapper = leaveApplicationMapper;
        this.approvalStepMapper = approvalStepMapper;
        this.returnRequestMapper = returnRequestMapper;
    }

    /**
     * 创建请假申请实现逻辑
     * 1. 校验请假信息合法性
     * 2. 计算请假时长（分钟）和审批等级
     * 3. 初始化请假申请状态并持久化
     * 4. 创建对应审批等级的审批节点
     *
     * @param leaveApplication 请假申请实体
     * @return 持久化后的请假申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveApplication createLeave(LeaveApplication leaveApplication) {
        validateCreateLeave(leaveApplication);

        int durationMinutes = calculateDurationMinutes(leaveApplication.getStartTime(), leaveApplication.getEndTime());
        int approvalLevel = calculateApprovalLevel(durationMinutes);

        leaveApplication.setDurationMinutes(durationMinutes);
        leaveApplication.setApprovalLevel(approvalLevel);
        leaveApplication.setStatus(LeaveStatusEnum.PENDING_COUNSELOR.getDbStatus());

        leaveApplicationMapper.insert(leaveApplication);
        createApprovalSteps(leaveApplication.getId(), approvalLevel);

        return leaveApplicationMapper.selectById(leaveApplication.getId());
    }

    /**
     * 审批请假申请实现逻辑
     * 1. 校验假单存在性和审批合法性
     * 2. 获取当前待审批节点
     * 3. 根据审批动作（同意/拒绝）执行对应逻辑
     *
     * @param leaveId 请假申请ID
     * @param action  审批动作
     * @param comment 审批备注
     * @return 更新后的请假申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveApplication approveLeave(Long leaveId, String action, String comment) {
        LeaveApplication leaveApplication = getLeaveOrThrow(leaveId);
        assertLeaveCanBeApproved(leaveApplication);

        ApprovalStep currentStep = getCurrentPendingStep(leaveId);
        if (currentStep == null) {
            throw new IllegalStateException("当前假单没有待审批节点");
        }

        if (ACTION_REJECT.equalsIgnoreCase(action)) {
            rejectLeave(leaveApplication, currentStep, comment);
            return leaveApplicationMapper.selectById(leaveId);
        }

        if (!ACTION_APPROVE.equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("审批动作只能是 approve 或 reject");
        }

        approveCurrentStep(leaveApplication, currentStep, comment);
        return leaveApplicationMapper.selectById(leaveId);
    }

    /**
     * 确认销假实现逻辑
     * 1. 校验假单状态（仅待销假状态可操作）
     * 2. 更新待处理的销假申请状态为已同意
     * 3. 将假单状态更新为已完结
     *
     * @param leaveId 请假申请ID
     * @param comment 销假备注
     * @return 更新后的请假申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveApplication confirmReturn(Long leaveId, String comment) {
        LeaveApplication leaveApplication = getLeaveOrThrow(leaveId);
        if (!LeaveStatusEnum.LEAVE_APPROVED.getDbStatus().equals(leaveApplication.getStatus())) {
            throw new IllegalStateException("只有已通过且待销假的假单可以确认销假");
        }

        ReturnRequest pendingReturnRequest = getPendingReturnRequest(leaveId);
        if (pendingReturnRequest != null) {
            pendingReturnRequest.setStatus(STEP_APPROVED);
            pendingReturnRequest.setComment(StringUtils.hasText(comment) ? comment : "同意销假");
            returnRequestMapper.updateById(pendingReturnRequest);
        }

        leaveApplication.setStatus(LeaveStatusEnum.FINISHED.getDbStatus());
        leaveApplicationMapper.updateById(leaveApplication);
        return leaveApplicationMapper.selectById(leaveId);
    }

    /**
     * 同意当前审批节点逻辑
     * 1. 更新当前节点为已同意状态
     * 2. 判断是否为最后一个审批节点：
     *    - 是：更新假单为已通过状态
     *    - 否：激活下一个审批节点，更新假单为对应待审批状态
     *
     * @param leaveApplication 请假申请实体
     * @param currentStep      当前审批节点
     * @param comment          审批备注
     */
    private void approveCurrentStep(LeaveApplication leaveApplication, ApprovalStep currentStep, String comment) {
        int requiredLevel = calculateApprovalLevel(leaveApplication);
        int currentStepNo = currentStep.getStepNo();

        currentStep.setStatus(STEP_APPROVED);
        currentStep.setComment(StringUtils.hasText(comment) ? comment : "同意");
        currentStep.setDecidedAt(LocalDateTime.now());
        approvalStepMapper.updateById(currentStep);

        // 当前节点是最后一个审批节点，直接通过
        if (currentStepNo >= requiredLevel) {
            leaveApplication.setApprovalLevel(requiredLevel);
            leaveApplication.setStatus(LeaveStatusEnum.LEAVE_APPROVED.getDbStatus());
            leaveApplicationMapper.updateById(leaveApplication);
            return;
        }

        // 激活下一个审批节点
        int nextStepNo = currentStepNo + 1;
        ensureApprovalStepExists(leaveApplication.getId(), nextStepNo);
        activateNextStep(leaveApplication.getId(), nextStepNo);

        // 更新假单状态为下一个节点的待审批状态
        leaveApplication.setApprovalLevel(requiredLevel);
        leaveApplication.setStatus(LeaveStatusEnum.pendingByStepNo(nextStepNo).getDbStatus());
        leaveApplicationMapper.updateById(leaveApplication);
    }

    /**
     * 拒绝当前请假申请逻辑
     * 1. 更新当前审批节点为已拒绝状态
     * 2. 终止所有待激活的审批节点
     * 3. 更新假单为已拒绝状态
     *
     * @param leaveApplication 请假申请实体
     * @param currentStep      当前审批节点
     * @param comment          审批备注
     */
    private void rejectLeave(LeaveApplication leaveApplication, ApprovalStep currentStep, String comment) {
        currentStep.setStatus(STEP_REJECTED);
        currentStep.setComment(StringUtils.hasText(comment) ? comment : "不同意请假");
        currentStep.setDecidedAt(LocalDateTime.now());
        approvalStepMapper.updateById(currentStep);

        // 终止所有待激活的审批节点
        ApprovalStep waiting = new ApprovalStep();
        waiting.setStatus(STEP_REJECTED);
        waiting.setComment("上级节点已终止");
        waiting.setDecidedAt(LocalDateTime.now());
        approvalStepMapper.update(waiting, new UpdateWrapper<ApprovalStep>()
                .eq("leave_id", leaveApplication.getId())
                .eq("status", STEP_WAITING));

        // 更新假单状态为已拒绝
        leaveApplication.setStatus(LeaveStatusEnum.APPROVAL_REJECTED.getDbStatus());
        leaveApplicationMapper.updateById(leaveApplication);
    }

    /**
     * 创建审批节点
     * 根据审批等级批量创建对应数量的审批节点，第一个节点为待审批状态，其余为待激活状态
     *
     * @param leaveId       请假申请ID
     * @param approvalLevel 审批等级（需要创建的节点数）
     */
    private void createApprovalSteps(Long leaveId, int approvalLevel) {
        for (int stepNo = 1; stepNo <= approvalLevel; stepNo++) {
            ApprovalStep step = buildApprovalStep(leaveId, stepNo);
            step.setStatus(stepNo == 1 ? STEP_PENDING : STEP_WAITING);
            approvalStepMapper.insert(step);
        }
    }

    /**
     * 确保审批节点存在
     * 若指定编号的审批节点不存在，则创建该节点（待激活状态）
     *
     * @param leaveId  请假申请ID
     * @param stepNo   审批节点编号
     */
    private void ensureApprovalStepExists(Long leaveId, int stepNo) {
        ApprovalStep existing = approvalStepMapper.selectOne(new QueryWrapper<ApprovalStep>()
                .eq("leave_id", leaveId)
                .eq("step_no", stepNo)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }

        ApprovalStep step = buildApprovalStep(leaveId, stepNo);
        step.setStatus(STEP_WAITING);
        approvalStepMapper.insert(step);
    }

    /**
     * 构建审批节点实体
     * 根据节点编号设置对应审批人信息（1级：辅导员，2级：党委副书记，3级：分管副院长）
     *
     * @param leaveId 请假申请ID
     * @param stepNo  审批节点编号
     * @return 审批节点实体
     */
    private ApprovalStep buildApprovalStep(Long leaveId, int stepNo) {
        ApprovalStep step = new ApprovalStep();
        step.setLeaveId(leaveId);
        step.setStepNo(stepNo);
        step.setStatus(STEP_WAITING);

        if (stepNo == 1) {
            step.setApproverRole("审批人(辅导员)");
            step.setApproverName("LIHUANYU");
            step.setApproverCode("HUANYU");
            return step;
        }

        if (stepNo == 2) {
            step.setApproverRole("审批人(学院党委副书记)");
            step.setApproverName("WANGJIE");
            step.setApproverCode("JIE");
            return step;
        }

        step.setApproverRole("审批人(学院分管副院长)");
        step.setApproverName("ZHANGQIANG");
        step.setApproverCode("QIANG");
        return step;
    }

    /**
     * 激活下一个审批节点
     * 将指定编号的审批节点状态从待激活更新为待审批
     *
     * @param leaveId 请假申请ID
     * @param nextStepNo 下一个审批节点编号
     */
    private void activateNextStep(Long leaveId, int nextStepNo) {
        ApprovalStep nextStep = approvalStepMapper.selectOne(new QueryWrapper<ApprovalStep>()
                .eq("leave_id", leaveId)
                .eq("step_no", nextStepNo)
                .last("LIMIT 1"));
        if (nextStep == null) {
            throw new IllegalStateException("下一审批节点不存在");
        }

        nextStep.setStatus(STEP_PENDING);
        approvalStepMapper.updateById(nextStep);
    }

    /**
     * 获取当前待审批节点
     * 查询请假申请下状态为待审批的第一个节点
     *
     * @param leaveId 请假申请ID
     * @return 当前待审批节点（无则返回null）
     */
    private ApprovalStep getCurrentPendingStep(Long leaveId) {
        return approvalStepMapper.selectOne(new QueryWrapper<ApprovalStep>()
                .eq("leave_id", leaveId)
                .eq("status", STEP_PENDING)
                .orderByAsc("step_no")
                .last("LIMIT 1"));
    }

    /**
     * 获取待处理的销假申请
     * 查询请假申请下状态为待审批的最新销假申请
     *
     * @param leaveId 请假申请ID
     * @return 待处理销假申请（无则返回null）
     */
    private ReturnRequest getPendingReturnRequest(Long leaveId) {
        return returnRequestMapper.selectOne(new QueryWrapper<ReturnRequest>()
                .eq("leave_id", leaveId)
                .eq("status", STEP_PENDING)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
    }

    /**
     * 获取请假申请（不存在则抛出异常）
     * 校验请假ID非空，且假单存在
     *
     * @param leaveId 请假申请ID
     * @return 请假申请实体
     * @throws IllegalArgumentException ID为空/假单不存在时抛出
     */
    private LeaveApplication getLeaveOrThrow(Long leaveId) {
        if (leaveId == null) {
            throw new IllegalArgumentException("假单ID不能为空");
        }
        LeaveApplication leaveApplication = leaveApplicationMapper.selectById(leaveId);
        if (leaveApplication == null) {
            throw new IllegalArgumentException("假单不存在");
        }
        return leaveApplication;
    }

    /**
     * 校验创建请假申请的参数合法性
     * 校验项：实体非空、学号非空、姓名非空、时间非空、结束时间晚于开始时间
     *
     * @param leaveApplication 请假申请实体
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    private void validateCreateLeave(LeaveApplication leaveApplication) {
        if (leaveApplication == null) {
            throw new IllegalArgumentException("请假信息不能为空");
        }
        if (!StringUtils.hasText(leaveApplication.getStudentId())) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (!StringUtils.hasText(leaveApplication.getStudentName())) {
            throw new IllegalArgumentException("学生姓名不能为空");
        }
        if (leaveApplication.getStartTime() == null || leaveApplication.getEndTime() == null) {
            throw new IllegalArgumentException("请假开始时间和结束时间不能为空");
        }
        if (!leaveApplication.getEndTime().isAfter(leaveApplication.getStartTime())) {
            throw new IllegalArgumentException("请假结束时间必须晚于开始时间");
        }
    }

    /**
     * 校验假单是否可审批
     * 禁止审批的状态：已拒绝、已完结、已取消、已通过
     *
     * @param leaveApplication 请假申请实体
     * @throws IllegalStateException 假单不可审批时抛出
     */
    private void assertLeaveCanBeApproved(LeaveApplication leaveApplication) {
        if (LeaveStatusEnum.APPROVAL_REJECTED.getDbStatus().equals(leaveApplication.getStatus())
                || LeaveStatusEnum.FINISHED.getDbStatus().equals(leaveApplication.getStatus())
                || LeaveStatusEnum.CANCELLED.getDbStatus().equals(leaveApplication.getStatus())) {
            throw new IllegalStateException("当前假单流程已终结，不能继续审批");
        }
        if (LeaveStatusEnum.LEAVE_APPROVED.getDbStatus().equals(leaveApplication.getStatus())) {
            throw new IllegalStateException("当前假单已审批通过，不能重复审批");
        }
    }

    /**
     * 计算请假时长（分钟）
     * 校验时长大于0且不超出Integer最大值
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 请假时长（分钟）
     * @throws IllegalArgumentException 时长不合法时抛出
     */
    private int calculateDurationMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            throw new IllegalArgumentException("请假时长必须大于0分钟");
        }
        if (minutes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("请假时长超出系统允许范围");
        }
        return (int) minutes;
    }

    /**
     * 计算审批等级（重载方法）
     * 从请假申请中获取时长并计算审批等级
     *
     * @param leaveApplication 请假申请实体
     * @return 审批等级（1/2/3）
     */
    private int calculateApprovalLevel(LeaveApplication leaveApplication) {
        if (leaveApplication.getDurationMinutes() != null && leaveApplication.getDurationMinutes() > 0) {
            return calculateApprovalLevel(leaveApplication.getDurationMinutes());
        }
        return calculateApprovalLevel(calculateDurationMinutes(leaveApplication.getStartTime(), leaveApplication.getEndTime()));
    }

    /**
     * 计算审批等级
     * 规则：≤3天→1级，≤10天→2级，>10天→3级（1天=1440分钟）
     *
     * @param durationMinutes 请假时长（分钟）
     * @return 审批等级（1/2/3）
     */
    private int calculateApprovalLevel(int durationMinutes) {
        int days = (durationMinutes + 1439) / 1440; // 向上取整计算天数
        if (days <= 3) {
            return 1;
        }
        if (days <= 10) {
            return 2;
        }
        return 3;
    }
}