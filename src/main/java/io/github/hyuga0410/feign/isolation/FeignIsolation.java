package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.string.StringUtil;
import io.github.hyuga0410.feign.proxy.FeignProxyConstants;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * feign isolation注解
 *
 * @author hyuga
 * @since 2022/01/07
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({FeignIsolationConfiguration.class, FeignIsolationCore.class})
public @interface FeignIsolation {

    /**
     * 启用环境
     * 在{@link FeignIsolationCore}判断是否需要注入
     */
    String[] environments() default {FeignProxyConstants.DEV};

    /**
     * 默认环境，做为允许所有跨组服务调用的服务器IP
     */
    String defaultIp() default StringUtil.EMPTY;

    /**
     * 强制隔离IPS，比如测试环境，各环境服务必须强制隔离
     * <p>
     * #拼接
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
    String serviceSign();

}
