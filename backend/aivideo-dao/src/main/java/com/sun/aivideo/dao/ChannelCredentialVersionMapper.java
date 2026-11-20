package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.aivideo.entity.ChannelCredentialVersion;
import org.apache.ibatis.annotations.Param;

public interface ChannelCredentialVersionMapper extends BaseMapper<ChannelCredentialVersion> {
    ChannelCredentialVersion findCurrent(@Param("userId") Long userId, @Param("channel") String channel);

    Long findEnabledVersionForUpdate(@Param("userId") Long userId, @Param("channel") String channel);

    ChannelCredentialVersion findForTask(@Param("userId") Long userId, @Param("channel") String channel,
                                         @Param("versionId") Long versionId);

    int retireCurrent(@Param("userId") Long userId, @Param("channel") String channel);

    boolean hasActiveReferences(@Param("userId") Long userId, @Param("channel") String channel);

    int purgeUnusedSecrets(@Param("userId") Long userId, @Param("channel") String channel);
}
