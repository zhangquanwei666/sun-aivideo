package com.sun.aivideo.service;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.common.enums.AiChannel;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.config.CredentialCipher;
import com.sun.aivideo.dao.*;
import com.sun.aivideo.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service @RequiredArgsConstructor
public class ChannelCredentialService {
    private final ChannelCredentialMapper credentials;
    private final ChannelCredentialVersionMapper versions;
    private final UserMapper users;
    private final CredentialCipher cipher;

    public record Summary(String channel, String name, boolean configured, boolean enabled,
                          String maskedKey, LocalDateTime updatedAt) {}

    public List<Summary> list(Long userId) {
        return Arrays.stream(AiChannel.values()).map(channel -> {
            ChannelCredential c = credentials.find(userId, channel.name());
            ChannelCredentialVersion v = c == null || c.getCurrentVersionId() == null ? null : versions.selectById(c.getCurrentVersionId());
            boolean configured = v != null && v.getCiphertext() != null;
            return new Summary(channel.name(), channel.getLabel(), configured,
                    configured && Boolean.TRUE.equals(c.getEnabled()), configured ? "••••" + v.getKeySuffix() : null,
                    c == null ? null : c.getUpdatedAt());
        }).toList();
    }

    /** 所有修改及任务绑定均在事务内锁用户，避免删除与创建任务竞争。 */
    public void lockUser(Long userId) {
        User user = users.selectByIdForUpdate(userId);
        if (user == null || !"ENABLED".equals(user.getStatus())) throw new BizException(ErrorCode.ACCOUNT_UNAVAILABLE);
    }

    public boolean configured(Long userId, AiChannel channel) {
        ChannelCredential c = credentials.find(userId, channel.name());
        return c != null && Boolean.TRUE.equals(c.getEnabled()) && c.getCurrentVersionId() != null;
    }

    public Long requiredVersion(Long userId, String channel) {
        AiChannel code = channel(channel);
        ChannelCredential c = credentials.find(userId, code.name());
        if (c == null || !Boolean.TRUE.equals(c.getEnabled()) || c.getCurrentVersionId() == null)
            throw new BizException(ErrorCode.CHANNEL_KEY_REQUIRED, code.getLabel());
        return c.getCurrentVersionId();
    }

    @Transactional
    public void save(Long userId, String channel, String apiKey) {
        AiChannel code = channel(channel);
        if (apiKey == null || !apiKey.matches("[!-~]{8,2048}") || apiKey.contains("••••"))
            throw new BizException(ErrorCode.INVALID_CHANNEL_KEY);
        lockUser(userId);
        ChannelCredential c = credentials.find(userId, code.name());
        if (c == null) {
            c = new ChannelCredential(); c.setUserId(userId); c.setChannelCode(code.name()); c.setEnabled(true);
            credentials.insert(c);
        }
        ChannelCredentialVersion v = new ChannelCredentialVersion();
        v.setCredentialId(c.getId()); v.setEncryptionKeyId(cipher.keyId());
        v.setCiphertext(cipher.encrypt(apiKey, userId + ":" + code.name()));
        v.setKeySuffix(apiKey.substring(apiKey.length() - 4));
        versions.insert(v);
        c.setCurrentVersionId(v.getId()); c.setEnabled(true); credentials.updateById(c);
        credentials.purgeUnusedSecrets(c.getId());
    }

    @Transactional
    public void setEnabled(Long userId, String channel, boolean enabled) {
        lockUser(userId);
        ChannelCredential c = own(userId, channel);
        if (c.getCurrentVersionId() == null) throw new BizException(ErrorCode.CHANNEL_KEY_REQUIRED, channel(channel).getLabel());
        c.setEnabled(enabled); credentials.updateById(c);
    }

    @Transactional
    public void delete(Long userId, String channel) {
        lockUser(userId);
        ChannelCredential c = own(userId, channel);
        if (credentials.activeReferences(c.getId()) > 0) throw new BizException(ErrorCode.CHANNEL_KEY_IN_USE);
        credentials.clearCurrent(c.getId()); credentials.purgeUnusedSecrets(c.getId());
    }

    /** 不检查配置是否启用：停用只阻止新任务，已绑定任务仍使用原版本。 */
    public ChannelCredentials forTask(Long userId, String channel, Long versionId) {
        AiChannel code = channel(channel);
        ChannelCredentialVersion v = versionId == null ? null : versions.selectById(versionId);
        ChannelCredential c = v == null ? null : credentials.selectById(v.getCredentialId());
        if (c == null || !userId.equals(c.getUserId()) || !code.name().equals(c.getChannelCode()) || v.getCiphertext() == null)
            throw new BizException(ErrorCode.CHANNEL_KEY_UNAVAILABLE);
        return new ChannelCredentials(code, cipher.decrypt(v.getCiphertext(), userId + ":" + code.name(), v.getEncryptionKeyId()));
    }

    @org.springframework.scheduling.annotation.Scheduled(initialDelay = 60000, fixedDelay = 86400000)
    public void purgeCompletedVersions() {
        credentials.purgeUnusedSecrets(null);
    }

    private ChannelCredential own(Long userId, String channel) {
        ChannelCredential c = credentials.find(userId, channel(channel).name());
        if (c == null) throw new BizException(ErrorCode.NOT_FOUND);
        return c;
    }
    private AiChannel channel(String value) {
        try { return AiChannel.valueOf(value); }
        catch (Exception e) { throw new BizException(ErrorCode.INVALID_CHANNEL); }
    }
}
