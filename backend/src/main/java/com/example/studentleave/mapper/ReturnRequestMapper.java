package com.example.studentleave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studentleave.entity.ReturnRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 返校申请数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现返校申请表的CRUD基础操作
 * 处理学生申请返校的相关数据操作
 *
 * @author （可补充作者）
 * @since （可补充版本/创建时间）
 */
@Mapper
public interface ReturnRequestMapper extends BaseMapper<ReturnRequest> {
    // BaseMapper<ReturnRequest> 已提供基础CRUD方法，覆盖返校申请的增删改查
}