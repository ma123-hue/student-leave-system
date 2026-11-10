package com.example.studentleave.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 请假申请主表实体类
 * 对应数据表：leave_applications
 * 存储学生提交的完整请假申请信息
 * 马新烨 第二天提交
 */
@Data
@TableName("leave_applications")
public class LeaveApplication {
    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生学号
     */
    private String studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 是否离开晋中市
     */
    private Boolean isOutsideJinzhong;

    /**
     * 请假类型（事假/病假/公假等）
     */
    private String leaveType;

    /**
     * 地址层级信息
     */
    private String addressLevels;

    /**
     * 详细停留地址
     */
    private String detailedAddress;

    /**
     * 请假开始时间
     */
    private LocalDateTime startTime;

    /**
     * 请假结束时间
     */
    private LocalDateTime endTime;

    /**
     * 请假总时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 请假原因描述
     */
    private String reason;

    /**
     * 附件信息JSON字符串
     */
    private String attachmentsJson;

    /**
     * 学生联系电话
     */
    private String studentPhone;

    /**
     * 紧急联系人姓名
     */
    private String emergencyContactName;

    /**
     * 紧急联系人电话
     */
    private String emergencyContactPhone;

    /**
     * 当前审批层级
     */
    private Integer approvalLevel;

    /**
     * 申请整体状态
     */
    private String status;

    /**
     * 申请提交时间
     */
    private LocalDateTime createdAt;

    /**
     * 信息最后更新时间
     */
    private LocalDateTime updatedAt;
}