package com.example.studentleave.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 返校申请实体类
 * 对应数据表：return_requests
 * 学生提前返校/正常返校登记申请
 * 马新烨 第二天提交
 */
@Data
@TableName("return_requests")
public class ReturnRequest {
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
     * 实际返校时间
     */
    private LocalDateTime returnTime;

    /**
     * 备注说明
     */
    private String note;

    /**
     * 返校申请状态
     */
    private String status;

    /**
     * 审批意见
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