package com.example.studentleave.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 返校延期申请实体类
 * 对应数据表：return_extension_requests
 * 学生申请延长返校时间的相关记录
 * 马新烨 第二天提交
 */
@Data
@TableName("return_extension_requests")
public class ReturnExtensionRequest {
    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联原请假单ID
     */
    private Long leaveId;

    /**
     * 学生学号
     */
    private String studentId;

    /**
     * 原计划返校时间
     */
    private LocalDateTime oldReturnTime;

    /**
     * 新申请返校时间
     */
    private LocalDateTime newReturnTime;

    /**
     * 延期原因
     */
    private String reason;

    /**
     * 当前审批层级
     */
    private Integer approvalLevel;

    /**
     * 当前审批步骤
     */
    private Integer currentStep;

    /**
     * 申请状态
     */
    private String status;

    /**
     * 审批备注意见
     */
    private String comment;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    private LocalDateTime updatedAt;
}