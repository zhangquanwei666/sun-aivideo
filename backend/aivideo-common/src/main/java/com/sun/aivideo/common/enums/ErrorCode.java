package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 统一业务错误码和消息模板。保留现有响应码，同类错误可共用错误码。 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    INVALID_CHANNEL(400, "厂商渠道未接入"),
    INVALID_CHANNEL_KEY(400, "密钥格式不正确，请输入完整密钥（8~2048 位，不含空白）"),
    CHANNEL_KEY_REQUIRED(400, "请先在模型密钥中配置并启用%s的 Key"),
    CHANNEL_KEY_IN_USE(400, "此厂商仍有未完成任务，请等待任务结束后再删除密钥"),
    CHANNEL_KEY_UNAVAILABLE(400, "任务绑定的密钥不可用，请检查配置；历史任务不会自动使用新账户重试"),
    UNAUTHORIZED(401, "请先登录或登录已过期"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "资源不存在"),
    PROVIDER_ERROR(502, "AI 服务调用失败"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    DATABASE_ERROR(500, "数据库访问失败"),
    MALFORMED_REQUEST(400, "请求体格式错误"),
    UPLOAD_TOO_LARGE(400, "上传文件过大"),
    INVALID_PARAMETER(400, "参数 %s 格式不正确"),
    PASSWORD_TOO_LONG(400, "密码不能超过72字节"),
    USERNAME_EXISTS(400, "用户名已存在"),
    INVALID_CREDENTIALS(401, "用户名或密码错误"),
    ACCOUNT_DISABLED(403, "账号已禁用"),
    ACCOUNT_UNAVAILABLE(401, "账号不可用，请重新登录"),
    VIDEO_IN_PROGRESS(400, "视频还在生成中，完成后才能删除"),
    IMAGE_IN_PROGRESS(400, "图片还在生成中，完成后才能删除"),
    PROMPT_REQUIRED(400, "文生视频需要填写提示词"),
    FIRST_FRAME_REQUIRED(400, "图生视频需要上传首帧图片"),
    INVALID_FIRST_FRAME(400, "首帧图片需为 http(s) 公网地址或上传后的 /files/ 路径"),
    REFERENCE_UPLOAD_REQUIRED(400, "参考图需要先通过上传接口上传"),
    REFERENCE_SIZE_UNSUPPORTED(400, "万相使用参考图时最高支持 2K，请改用 1K 或 2K"),
    IMAGE_REQUIRED(400, "请选择图片"),
    IMAGE_TOO_LARGE(400, "图片不能超过 10MB"),
    IMAGE_TYPE_UNSUPPORTED(400, "仅支持 JPG / PNG / WEBP / BMP 图片"),
    EMPTY_PROVIDER_IMAGE(502, "厂商返回的图片为空"),
    IMAGE_SAVE_INTERRUPTED(500, "保存生成的图片被中断"),
    VIDEO_NOT_FOUND(404, "任务不存在：%s"),
    VIDEO_DELETED(404, "任务不存在或已删除：%s"),
    IMAGE_NOT_FOUND(404, "图片任务不存在：%s"),
    IMAGE_DELETED(404, "图片任务不存在或已删除：%s"),
    MODEL_NOT_FOUND(400, "模型不存在或未启用：%s"),
    IMAGE_MODEL_NOT_FOUND(400, "文生图模型不存在或未启用：%s"),
    IMAGE_PROVIDER_UNSUPPORTED(400, "文生图厂商尚未接入：%s"),
    LOCAL_IMAGE_NOT_FOUND(400, "图片不存在：%s"),
    DOWNLOAD_IMAGE_FAILED(502, "下载生成的图片失败：HTTP %s"),
    RESOLUTION_RATIO_UNSUPPORTED(400, "%s 的 %s 不支持画幅 %s"),
    MODE_UNSUPPORTED(400, "%s 不支持%s"),
    RESOLUTION_UNSUPPORTED(400, "%s 只支持分辨率：%s"),
    VIDEO_PRICE_MISSING(400, "未配置 %s 的单价，请先在 t_model_config 中补充"),
    FIRST_FRAME_DURATION(400, "%s 使用首帧图片时最长 10 秒"),
    RATIO_UNSUPPORTED(400, "%s 只支持画幅：%s"),
    IMAGE_SIZE_UNSUPPORTED(400, "%s 只支持清晰度：%s"),
    IMAGE_PRICE_MISSING(400, "未配置 %s 的单价，请先在 t_image_model_config 中补充"),
    SAVE_IMAGE_FAILED(500, "保存图片失败：%s"),
    READ_IMAGE_FAILED(500, "读取图片失败：%s"),
    SAVE_VIDEO_FAILED(500, "保存视频失败：%s"),
    SAVE_GENERATED_IMAGE_FAILED(500, "保存生成的图片失败：%s"),
    DURATION_UNSUPPORTED(400, "%s 时长范围为 %d~%d 秒"),
    REFERENCE_UNSUPPORTED(400, "%s 不支持参考图"),
    TOO_MANY_REFERENCES(400, "%s 最多支持 %d 张参考图"),

    ADMIN_INVALID_CREDENTIALS(401, "管理员用户名或密码错误"),
    ADMIN_DISABLED(403, "管理员账号已禁用"),
    ADMIN_UNAVAILABLE(401, "管理员账号不可用，请重新登录"),
    USER_NOT_FOUND(404, "用户不存在：%s"),
    INVALID_USER_STATUS(400, "用户状态只支持 ENABLED / DISABLED"),
    INVALID_FEED_TYPE(400, "记录类型只支持 VIDEO / IMAGE");

    private final int code;
    private final String message;

    public String format(Object... args) {
        return args.length == 0 ? message : String.format(java.util.Locale.ROOT, message, args);
    }
}
