package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** 不生成 toString，避免数据库密文进入日志。 */
@Getter @Setter
@TableName("t_user_channel_credential_version")
public class ChannelCredentialVersion {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String channelCode;
    /** 1 为当前版本，NULL 为历史版本。 */
    private Integer currentSlot;
    private Boolean enabled;
    private String ciphertext;
    private String encryptionKeyId;
    private String keySuffix;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
