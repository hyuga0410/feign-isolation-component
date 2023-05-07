package io.github.hyuga0410.feign.isolation;

/**
 * FeignIsolationConstants
 *
 * @author hyuga
 * @since 2023/5/5-05-05 17:39
 */
public interface FeignIsolationConstants {

    /**
     * 服务名追加定制化后缀的标识符
     */
    char ISOLATION_SYMBOL = '.';

    /**
     * Redis中存放服务名的key前缀
     */
    String FEIGN_REDIS_KEY_PREFIX = "FEIGN_ISOLATION:";

    /**
     * SpringBoot项目名配置路径
     */
    String SPRING_APPLICATION_NAME = "spring.application.name";

}
