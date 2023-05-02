package io.github.hyuga0410.feign.proxy;

import cn.hyugatool.core.string.StringUtil;

import java.lang.annotation.*;

/**
 * Feign服务提供者
 *
 * @author pengqinglong
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignProvider {

    /**
     * @return 服务提供者
     */
    String value() default StringUtil.EMPTY;

    /**
     * 启用环境
     */
    String[] environments() default {FeignProxyConstants.DEV};

}
