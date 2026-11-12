package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.aivideo.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员 Mapper。
 * 单表增删改、按主键查询用 BaseMapper 自带方法；自定义 SQL 在 resources/mapper/AdminMapper.xml。
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

    /** 按登录名查询。对应 XML：selectByUsername */
    Admin selectByUsername(@Param("username") String username);

    /** 管理员总数（启动时判断是否需要创建初始管理员）。对应 XML：countAll */
    long countAll();
}
