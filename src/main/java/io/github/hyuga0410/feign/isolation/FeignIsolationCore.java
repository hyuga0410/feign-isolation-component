package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.collection.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * feign isolation核心类
 *
 * @author pengqinglong
 * @since 2021/12/29
 */
@Component
@Slf4j
public class FeignIsolationCore implements ApplicationRunner {

    @Bean
    public FeignBuilderHelper feignBuilderHelper() {
        String[] environments = FeignIsolationConfiguration.environments();
        String[] profiles = environment.getActiveProfiles();
        long count = Arrays.stream(profiles).filter(profile -> ArrayUtil.contains(environments, profile)).count();
        boolean isolation = count > 0;
        return new FeignBuilderHelper(isolation);
    }

    @Resource
    private Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String[] environments = FeignIsolationConfiguration.environments();

        String[] profiles = environment.getActiveProfiles();

        long count = Arrays.stream(profiles).filter(profile -> ArrayUtil.contains(environments, profile)).count();
        if (count <= 0) {
            log.info("Feign isolation start fail,Environmental mismatch~~~");
            return;
        }

        log.info("Feign isolation start success~");
    }

}