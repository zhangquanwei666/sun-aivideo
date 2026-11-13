package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.aivideo.dao.bo.UserListBO;
import com.sun.aivideo.dao.bo.UserOverviewBO;
import com.sun.aivideo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 前台用户 Mapper。
 * 单表增删改、按主键查询用 BaseMapper 自带方法；自定义 SQL 在 resources/mapper/UserMapper.xml。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 按登录名查询（不含已删除）。对应 XML：selectByUsername */
    User selectByUsername(@Param("username") String username);

    /** 锁定用户，串行处理账号状态、凭证修改和任务创建。 */
    User selectByIdForUpdate(@Param("id") Long id);

    /**
     * 管理后台的用户列表，附带每个用户的视频数、图片数和累计费用。对应 XML：selectUserPage
     *
     * @param keyword 按登录名 / 昵称模糊搜索，可为空
     * @param status  ENABLED / DISABLED，为空不过滤
     */
    IPage<UserListBO> selectUserPage(IPage<UserListBO> page,
                                     @Param("keyword") String keyword,
                                     @Param("status") String status);

    /** 用户总数、今日新增、已禁用数。对应 XML：selectOverview */
    UserOverviewBO selectOverview();
}
