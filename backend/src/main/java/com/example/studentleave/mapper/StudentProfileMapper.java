package com.example.studentleave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studentleave.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生档案数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现学生档案表的CRUD基础操作
 * 管理学生的基本信息（如姓名、班级、联系方式等）
 *
 * @author （可补充作者）
 * @since （可补充版本/创建时间）
 */
@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
    // BaseMapper<StudentProfile> 已提供基础CRUD方法，覆盖学生档案的增删改查
}