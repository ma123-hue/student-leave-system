package com.example.studentleave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studentleave.entity.LeaveApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假申请数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现请假申请表的CRUD基础操作
 * 处理学生各类请假申请（事假、病假等）的数据操作
 *
 * @author （可补充作者）
 * @since （可补充版本/创建时间）
 */
@Mapper
public interface LeaveApplicationMapper extends BaseMapper<LeaveApplication> {
    // BaseMapper<LeaveApplication> 已提供基础CRUD方法，覆盖请假申请的增删改查
}