package com.zzyl.framework.interceptor;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zzyl.common.core.domain.model.LoginUser;
import com.zzyl.common.utils.DateUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

import static com.zzyl.common.utils.SecurityUtils.getLoginUser;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 是否排除创建人、修改人的自动填充。
     *
     * true：
     * 1. 小程序 /member 请求
     * 2. AMQP、定时任务、异步线程等非 HTTP 场景
     *
     * false：
     * 后台管理端正常 HTTP 请求
     */
    public boolean isExclude() {

        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();

        // AMQP、定时任务、异步线程等场景没有 HttpServletRequest
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return true;
        }

        HttpServletRequest request =
                ((ServletRequestAttributes) requestAttributes).getRequest();

        String requestURI = request.getRequestURI();

        return requestURI.startsWith("/member");
    }

    @Override
    public void insertFill(MetaObject metaObject) {

        // 创建时间无论什么场景都填充
        this.strictInsertFill(
                metaObject,
                "createTime",
                Date.class,
                DateUtils.getNowDate()
        );

        // 只有后台管理端 HTTP 请求才填充当前登录人
        if (!isExclude()) {
            this.strictInsertFill(
                    metaObject,
                    "createBy",
                    String.class,
                    String.valueOf(getLoginUserId())
            );
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {

        // 修改时间无论什么场景都填充
        this.setFieldValByName(
                "updateTime",
                new Date(),
                metaObject
        );

        // 只有后台管理端 HTTP 请求才填充当前登录人
        if (!isExclude()) {
            this.setFieldValByName(
                    "updateBy",
                    String.valueOf(getLoginUserId()),
                    metaObject
            );
        }
    }

    /**
     * 获取当前登录人的ID
     *
     * @return 登录人ID
     */
    public Long getLoginUserId() {
        try {
            LoginUser loginUser = getLoginUser();

            if (ObjectUtils.isNotEmpty(loginUser)) {
                return loginUser.getUserId();
            }

            return 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}