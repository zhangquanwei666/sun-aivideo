package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.aivideo.dao.bo.MemberRecordBO;
import com.sun.aivideo.entity.MemberRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员变更记录 Mapper。
 * 插入用 BaseMapper#insert；自定义 SQL 在 resources/mapper/MemberRecordMapper.xml。
 */
@Mapper
public interface MemberRecordMapper extends BaseMapper<MemberRecord> {

    /**
     * 分页查询会员变更记录，附带用户的登录名和昵称。对应 XML：selectRecordPage
     *
     * @param userId 只查该用户的记录，为空查全部
     */
    IPage<MemberRecordBO> selectRecordPage(IPage<MemberRecordBO> page, @Param("userId") Long userId);
}
