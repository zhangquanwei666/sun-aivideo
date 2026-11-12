package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.aivideo.dao.bo.UserListBO;
import com.sun.aivideo.dao.bo.UserOverviewBO;
import com.sun.aivideo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 前台用户 Mapper。
 * 单表增删改、按主键查询用 BaseMapper 自带方法；自定义 SQL 在 resources/mapper/UserMapper.xml。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 按登录名查询（不含已删除）。对应 XML：selectByUsername */
    User selectByUsername(@Param("username") String username);

    /** 管理操作时锁定用户，防止并发会员变更互相覆盖。 */
    User selectByIdForUpdate(@Param("id") Long id);

    /** 更新会员等级和到期时间；expireAt 允许显式写 NULL。 */
    int updateMembership(@Param("id") Long id,
                         @Param("memberLevel") String memberLevel,
                         @Param("expireAt") LocalDateTime expireAt);

    /**
     * 管理后台的用户列表，附带每个用户的视频数、图片数和累计费用。对应 XML：selectUserPage
     *
     * @param keyword 按登录名 / 昵称模糊搜索，可为空
     * @param member  VIP 只查有效会员，NORMAL 只查非会员（含已过期），为空不过滤
     * @param status  ENABLED / DISABLED，为空不过滤
     */
    IPage<UserListBO> selectUserPage(IPage<UserListBO> page,
                                     @Param("keyword") String keyword,
                                     @Param("member") String member,
                                     @Param("status") String status);

    /** 用户总数、有效会员数、今日新增、已禁用数。对应 XML：selectOverview */
    UserOverviewBO selectOverview();
}
