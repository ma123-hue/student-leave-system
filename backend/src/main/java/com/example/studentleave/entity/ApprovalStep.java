package com.example.studentleave.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批流程步骤实体类
 * 对应数据表：approval_steps
 * 记录每一条请假申请对应的逐级审批信息、审批人及审批结果
 * 马新烨 第二天提交
 */
@Data
@TableName("approval_steps")
public class ApprovalStep {
    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联请假申请表ID
     */
    private Long leaveId;

    /**
     * 审批步骤序号，区分多级审批顺序
     */
    private Integer stepNo;

    /**
     * 审批人角色（辅导员/系主任/院领导等）
     */
    private String approverRole;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人账号/工号
     */
    private String approverCode;

    /**
     * 当前步骤审批状态（待审批/已通过/已驳回）
     */
    private String status;

    /**
     * 审批意见/备注
     */
    private String comment;

    /**
     * 审批操作时间
     */
    private LocalDateTime decidedAt;

    /**
     * 本条记录创建时间
     */
    private LocalDateTime createdAt;
}