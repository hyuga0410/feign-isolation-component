package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.string.StringUtil;
import io.github.hyuga0410.feign.EnvironmentConstants;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * FeignIsolation注解
 *
 * @author hyuga
 * @since 2022/01/07
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({FeignIsolationConfiguration.class, FeignIsolationCore.class})
@SuppressWarnings("unused")
public @interface FeignIsolation {

    /**
     * 启用环境
     * <p>
     * 在{@link FeignIsolationCore}判断是否需要注入
     * <p>
     * 注意：切勿用于生产环境，初衷只用于测试环境
     */
    String[] environments() default {EnvironmentConstants.DEV};

    /**
     * 默认环境，做为允许所有跨组服务调用的服务器IP
     */
    String defaultIp() default StringUtil.EMPTY;

    /**
     * 强制隔离IPS，比如多个测试环境，各环境服务必须强制隔离
     * <p>
     * 多IP使用#拼接
     */
    String isolationIps() default StringUtil.EMPTY;

    /**
     * 允许跳过隔离限制的服务，比如多个测试环境中，各环境所使用的公共服务，可以配置到这里进行跳过隔离
     * <p>
     * 服务IP在isolationIps中，且服务名配置在skipIsolationServices时，则直接调用默认服务
     * <p>
     * #拼接
     */
    String skipIsolationServices() default StringUtil.EMPTY;

    /**
     * 指定隔离的服务名标识 请求的service服务名中包含该标识才进行隔离
     */
    String serviceSign() default StringUtil.EMPTY;

    /**
     * Redis地址配置路径
     */
    String redisUrlPath();

    /**
     * Redis端口配置路径
     */
    String redisPortPath();

    /**
     * Redis用户名配置路径
     */
    String redisUserPath() default StringUtil.EMPTY;

    /**
     * Redis密码配置路径
     */
    String redisPwdPath() default StringUtil.EMPTY;

}
