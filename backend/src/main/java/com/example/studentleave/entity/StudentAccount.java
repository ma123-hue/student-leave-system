package com.example.studentleave.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生账号实体类
 * 对应数据表：student_accounts
 * 存储学生登录账号、密码及密码状态
 * 马新烨 第二天提交
 */
@Data
@TableName("student_accounts")
public class StudentAccount {
    /**
     * 主键/学生学号，唯一标识
     */
    @TableId
    private String studentId;

    /**
     * 加密后的密码哈希值
     */
    private String passwordHash;

    /**
     * 密码加密盐值
     */
    private String passwordSalt;

    /**
     * 是否需要强制修改初始密码
     */
    private Boolean mustChangePassword;

    /**
     * 账号创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 账号信息最后更新时间
     */
    private LocalDateTime updatedAt;
}