package com.example.studentleave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studentleave.entity.StudentAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生账户数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现学生账户表的CRUD基础操作
 * 无需手动编写常规SQL，MyBatis-Plus自动生成
 *
 * @author （可补充作者）
 * @since （可补充版本/创建时间）
 */
@Mapper // 标识为MyBatis的Mapper接口，让Spring容器扫描并创建代理对象
public interface StudentAccountMapper extends BaseMapper<StudentAccount> {
    // BaseMapper<StudentAccount> 已提供：
    // insert(StudentAccount) - 新增学生账户
    // deleteById(Serializable) - 根据主键删除
    // updateById(StudentAccount) - 根据主键更新
    // selectById(Serializable) - 根据主键查询
    // selectList(Wrapper) - 条件查询列表 等基础方法
}