package com.zzyl.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author hinana
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 全部护理等级缓存 key
     */
    public static final  String NURSING_LEVEL_ALL_KEY="nursingLevel:all";

    /**
     * 获取房间信息缓存 key
     */
    public static final  String ROOM_DATA_KEY="elder:room:one";

    /**
     * 全部护理项目缓存 key
     */
    public static final  String NURSING_PROJECT_ALL_KEY="nursingProject:all";

    /**
     * 报告URL 缓存 key
     */
    public static final String REPORT_KEY = "health:report:";


    public static final String IOT_ALL_PRODUCT_LIST = "iot:all_product_list";
    public static final String IOT_DEVICE_LAST_DATA = "iot:device_last_data";
    /**
     * 报警规则需要的连续触发次数，缓存前缀
     */
    public static final String ALERT_TRIGGER_COUNT_PREFIX = "iot:alert_trigger_count:";
    /**
     * 报警规则沉默周期，缓存前缀
     */
    public static final String ALERT_SILENT_PREFIX = "iot:alert_silent:";
}
