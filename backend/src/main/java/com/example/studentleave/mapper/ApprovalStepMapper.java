package com.example.studentleave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studentleave.entity.ApprovalStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批步骤数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现审批步骤表的CRUD基础操作
 * 用于管理请假/返校等流程的审批节点数据
 *
 */
@Mapper
public interface ApprovalStepMapper extends BaseMapper<ApprovalStep> {
    // BaseMapper<ApprovalStep> 已提供基础CRUD方法，覆盖审批步骤的增删改查
}