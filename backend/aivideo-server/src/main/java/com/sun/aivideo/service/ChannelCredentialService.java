package com.sun.aivideo.service;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.common.enums.AiChannel;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.config.CredentialCipher;
import com.sun.aivideo.dao.ChannelCredentialVersionMapper;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.entity.ChannelCredentialVersion;
import com.sun.aivideo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service @RequiredArgsConstructor
public class ChannelCredentialService {
    private final ChannelCredentialVersionMapper versions;
    private final UserMapper users;
    private final CredentialCipher cipher;

    public record Summary(String channel, String name, boolean configured, boolean enabled,
                          String maskedKey, LocalDateTime updatedAt) {}

    public List<Summary> list(Long userId) {
        return Arrays.stream(AiChannel.values()).map(channel -> {
            ChannelCredentialVersion version = versions.findCurrent(userId, channel.name());
            boolean configured = version != null;
            return new Summary(channel.name(), channel.getLabel(), configured,
                    configured && Boolean.TRUE.equals(version.getEnabled()),
                    configured ? "••••" + version.getKeySuffix() : null,
                    configured ? version.getUpdatedAt() : null);
        }).toList();
    }

    /** 所有修改及任务绑定均在事务内锁用户，避免删除与创建任务竞争。 */
    public void lockUser(Long userId) {
        User user = users.selectByIdForUpdate(userId);
        if (user == null || !"ENABLED".equals(user.getStatus())) throw new BizException(ErrorCode.ACCOUNT_UNAVAILABLE);
    }

    public boolean configured(Long userId, AiChannel channel) {
        ChannelCredentialVersion version = versions.findCurrent(userId, channel.name());
        return version != null && Boolean.TRUE.equals(version.getEnabled());
    }

    public Long requiredVersion(Long userId, String channel) {
        AiChannel code = channel(channel);
        Long versionId = versions.findEnabledVersionForUpdate(userId, code.name());
        if (versionId == null) {
            throw new BizException(ErrorCode.CHANNEL_KEY_REQUIRED, code.getLabel());
        }
        return versionId;
    }

    @Transactional
    public void save(Long userId, String channel, String apiKey) {
        AiChannel code = channel(channel);
        if (apiKey == null || !apiKey.matches("[!-~]{8,2048}") || apiKey.contains("••••"))
            throw new BizException(ErrorCode.INVALID_CHANNEL_KEY);
        lockUser(userId);

        ChannelCredentialVersion version = new ChannelCredentialVersion();
        version.setUserId(userId);
        version.setChannelCode(code.name());
        version.setCurrentSlot(1);
        version.setEnabled(true);
        version.setEncryptionKeyId(cipher.keyId());
        version.setCiphertext(cipher.encrypt(apiKey, userId + ":" + code.name()));
        version.setKeySuffix(apiKey.substring(apiKey.length() - 4));

        versions.retireCurrent(userId, code.name());
        versions.insert(version);
        versions.purgeUnusedSecrets(userId, code.name());
    }

    @Transactional
    public void setEnabled(Long userId, String channel, boolean enabled) {
        AiChannel code = channel(channel);
        lockUser(userId);
        ChannelCredentialVersion current = versions.findCurrent(userId, code.name());
        if (current == null) {
            throw new BizException(ErrorCode.CHANNEL_KEY_REQUIRED, code.getLabel());
        }
        ChannelCredentialVersion update = new ChannelCredentialVersion();
        update.setId(current.getId());
        update.setEnabled(enabled);
        versions.updateById(update);
    }

    @Transactional
    public void delete(Long userId, String channel) {
        AiChannel code = channel(channel);
        lockUser(userId);
        if (versions.hasActiveReferences(userId, code.name())) {
            throw new BizException(ErrorCode.CHANNEL_KEY_IN_USE);
        }
        versions.retireCurrent(userId, code.name());
        versions.purgeUnusedSecrets(userId, code.name());
    }

    /** 不检查配置是否启用：停用只阻止新任务，已绑定任务仍使用原版本。 */
    public ChannelCredentials forTask(Long userId, String channel, Long versionId) {
        AiChannel code = channel(channel);
        ChannelCredentialVersion version = versionId == null ? null
                : versions.findForTask(userId, code.name(), versionId);
        if (version == null) {
            throw new BizException(ErrorCode.CHANNEL_KEY_UNAVAILABLE);
        }
        return new ChannelCredentials(code,
                cipher.decrypt(version.getCiphertext(), userId + ":" + code.name(), version.getEncryptionKeyId()));
    }

    @org.springframework.scheduling.annotation.Scheduled(initialDelay = 60000, fixedDelay = 86400000)
    public void purgeCompletedVersions() {
        versions.purgeUnusedSecrets(null, null);
    }
    private AiChannel channel(String value) {
        try { return AiChannel.valueOf(value); }
        catch (Exception e) { throw new BizException(ErrorCode.INVALID_CHANNEL); }
    }
}
