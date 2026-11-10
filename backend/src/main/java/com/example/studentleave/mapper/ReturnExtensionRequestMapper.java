package com.example.studentleave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studentleave.entity.ReturnExtensionRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 返校延期申请数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现返校延期申请表的CRUD基础操作
 * 处理学生申请延长返校时间的相关数据操作
 */
@Mapper
public interface ReturnExtensionRequestMapper extends BaseMapper<ReturnExtensionRequest> {
    // BaseMapper<ReturnExtensionRequest> 已提供基础CRUD方法，覆盖返校延期申请的增删改查
}