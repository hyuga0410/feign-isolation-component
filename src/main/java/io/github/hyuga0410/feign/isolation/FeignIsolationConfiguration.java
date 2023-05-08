package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.number.NumberUtil;
import cn.hyugatool.core.string.StringPoundSignUtil;
import cn.hyugatool.core.string.StringUtil;
import cn.hyugatool.system.NetworkUtil;
import cn.hyugatool.system.SystemUtil;
import io.github.hyuga0410.feign.proxy.FeignProxyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Feign Isolation Configuration
 *
 * @author hyuga
 * @since 2022/01/07
 */
@Component
@Slf4j
public class FeignIsolationConfiguration implements ImportBeanDefinitionRegistrar {

    // Server IP address

    private final String localIpAddr = NetworkUtil.getLocalIpAddr();

    // Configuration attributes

    private static String DEFAULT_IP;
    private static String SERVICE_SIGN;
    private static String[] ENVIRONMENTS;
    private static List<String> ISOLATION_IPS;
    private static List<String> SKIP_ISOLATION_SERVICES;
    private static String REDIS_URL_PATH;
    private static String REDIS_PORT_PATH;
    private static String REDIS_USER_PATH;
    private static String REDIS_PWD_PATH;

    // Configuration attribute acquisition method

    public static String defaultIp() {
        return DEFAULT_IP;
    }

    public static String serviceSign() {
        return SERVICE_SIGN;
    }

    public static String[] environments() {
        return ENVIRONMENTS;
    }

    public static List<String> isolationIps() {
        return ISOLATION_IPS;
    }

    public static List<String> skipIsolationServices() {
        return SKIP_ISOLATION_SERVICES;
    }

    public static String redisUrlPath() {
        return REDIS_URL_PATH;
    }

    public static String redisPortPath() {
        return REDIS_PORT_PATH;
    }

    public static String redisUserPath() {
        return REDIS_USER_PATH;
    }

    public static String redisPwdPath() {
        return REDIS_PWD_PATH;
    }

    /**
     * 根据导入{@code @Configuration}类的给定注释元数据，根据需要注册bean定义
     *
     * @param importingClassMetadata 导入类的注释元数据
     * @param registry               当前bean定义注册表
     */
    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        Map<String, Object> defaultAttrs = importingClassMetadata.getAnnotationAttributes(FeignIsolation.class.getName());
        if (defaultAttrs == null) {
            log.info("Feign isolation initialization failed ~~~");
            return;
        }

        DEFAULT_IP = (String) defaultAttrs.get("defaultIp");
        ENVIRONMENTS = (String[]) defaultAttrs.get("environments");
        SERVICE_SIGN = String.valueOf(defaultAttrs.get("serviceSign"));
        ISOLATION_IPS = StringPoundSignUtil.parseSign((String) defaultAttrs.get("isolationIps"));
        SKIP_ISOLATION_SERVICES = StringPoundSignUtil.parseSign((String) defaultAttrs.get("skipIsolationServices"));
        REDIS_URL_PATH = (String) defaultAttrs.get("redisUrlPath");
        REDIS_PORT_PATH = (String) defaultAttrs.get("redisPortPath");
        REDIS_USER_PATH = (String) defaultAttrs.get("redisUserPath");
        REDIS_PWD_PATH = (String) defaultAttrs.get("redisPwdPath");

        boolean isDefaultEnv = StringUtil.equals(DEFAULT_IP, localIpAddr);
        if (isDefaultEnv) {
            // 做为默认环境后缀为空
            System.setProperty(FeignProxyConstants.FEIGN_SUFFIX, StringUtil.EMPTY);
            return;
        }

        feignIsolation(localIpAddr);

        log.info("Feign isolation successful initialization ~~~");
    }

    /**
     * 进此方法所有服务都将启用隔离
     *
     * @param localIpAddr 当前服务IP
     */
    private void feignIsolation(String localIpAddr) {
        String hostName = SystemUtil.getLocalHostName();
        BigDecimal ipNumber = NumberUtil.getNumber(localIpAddr);

        String serviceIsolationSuffix = String.format(FeignIsolationConstants.ISOLATION_SYMBOL + "%s-%s", ipNumber, hostName);

        System.setProperty(FeignProxyConstants.FEIGN_SUFFIX, serviceIsolationSuffix);
    }

}