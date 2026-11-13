package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.aivideo.entity.ChannelCredential;
import org.apache.ibatis.annotations.Param;

public interface ChannelCredentialMapper extends BaseMapper<ChannelCredential> {
    ChannelCredential find(@Param("userId") Long userId, @Param("channel") String channel);
    int clearCurrent(@Param("id") Long id);
    long activeReferences(@Param("id") Long id);
    int purgeUnusedSecrets(@Param("id") Long id);
}
