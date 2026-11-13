package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user_channel_credential")
public class ChannelCredential {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String channelCode;
    private Boolean enabled;
    private Long currentVersionId;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
