package io.github.hyuga0410.feign.processor;

import cn.hyugatool.core.string.StringUtil;
import io.github.hyuga0410.feign.proxy.FeignProxyConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 环境后置处理程序
 * 本地启动test单测未读取application.yml导致启动异常问题解决
 *
 * @author chenyi
 * @since 2021/6/9
 */
public class FeignEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String feignSuffix = System.getProperty(FeignProxyConstants.FEIGN_SUFFIX);
        if (StringUtil.hasText(feignSuffix)) {
            return;
        }
        // 本地单元测试赋值spring.application.name后缀
        System.setProperty(FeignProxyConstants.FEIGN_SUFFIX, StringUtil.EMPTY);
    }

}

