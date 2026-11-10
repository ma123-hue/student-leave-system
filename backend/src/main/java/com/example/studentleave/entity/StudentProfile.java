package com.example.studentleave.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生档案信息实体类
 * 对应数据表：student_profiles
 * 存储学生基础个人信息、院系班级等资料
 * 马新烨 第二天提交
 */
@Data
@TableName("student_profiles")
public class StudentProfile {
    /**
     * 主键/学生学号
     */
    @TableId
    private String studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 性别
     */
    private String gender;

    /**
     * 所属学院
     */
    private String college;

    /**
     * 年级
     */
    private String grade;

    /**
     * 专业
     */
    private String major;

    /**
     * 班级
     */
    private String className;
}