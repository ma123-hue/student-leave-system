package com.example.studentleave.service;

import com.example.studentleave.entity.LeaveApplication;

/**
 * 请假申请核心业务接口
 * 定义请假申请的创建、审批、销假确认等核心业务操作
 *
 */
public interface LeaveApplicationService {

    /**
     * 创建请假申请
     * 校验请假信息合法性，计算请假时长和审批等级，初始化审批流程
     *
     * @param leaveApplication 请假申请实体（包含学号、姓名、请假时间等核心信息）
     * @return 持久化后的请假申请（包含生成的ID、审批等级等完整信息）
     * @throws IllegalArgumentException 请假信息不合法时抛出（如学号为空、结束时间早于开始时间等）
     */
    LeaveApplication createLeave(LeaveApplication leaveApplication);

    /**
     * 审批请假申请
     * 支持同意（approve）/拒绝（reject）两种操作，更新审批节点状态并推进审批流程
     *
     * @param leaveId 请假申请ID
     * @param action  审批动作（仅支持 approve/reject）
     * @param comment 审批备注（可为空，为空时默认填充"同意"/"不同意请假"）
     * @return 更新后的请假申请（包含最新审批状态、审批节点信息）
     * @throws IllegalArgumentException 请假ID为空/假单不存在/审批动作不合法时抛出
     * @throws IllegalStateException    假单已终结/已通过无法重复审批时抛出
     */
    LeaveApplication approveLeave(Long leaveId, String action, String comment);

    /**
     * 确认销假
     * 仅对已通过且待销假的假单生效，更新销假申请状态并完结假单流程
     *
     * @param leaveId 请假申请ID
     * @param comment 销假备注（可为空，为空时默认填充"同意销假"）
     * @return 更新后的请假申请（状态更新为已完结）
     * @throws IllegalArgumentException 请假ID为空/假单不存在时抛出
     * @throws IllegalStateException    假单非待销假状态时抛出
     */
    LeaveApplication confirmReturn(Long leaveId, String comment);
}