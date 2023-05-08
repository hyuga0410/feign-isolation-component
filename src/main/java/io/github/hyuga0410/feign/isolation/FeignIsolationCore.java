package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.collection.ArrayUtil;
import cn.hyugatool.core.lang.Assert;
import cn.hyugatool.extra.concurrent.SleuthThreadScheduledPool;
import cn.hyugatool.system.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.github.hyuga0410.feign.isolation.FeignIsolationConstants.FEIGN_REDIS_KEY_PREFIX;

/**
 * feign isolation核心类
 *
 * @author pengqinglong
 * @since 2021/12/29
 */
@Slf4j
@Component
public class FeignIsolationCore implements ApplicationRunner {

    @Resource
    private Environment environment;

    /**
     * 判断当前应用启动环境是否符合组件启用环境条件
     *
     * @return - true:环境变量命中启用隔离 - false:环境变量未命中不启用隔离
     */
    private boolean needIsolation() {
        final String[] environments = FeignIsolationConfiguration.environments();
        final String[] profiles = environment.getActiveProfiles();
        final long count = Arrays.stream(profiles).filter(profile -> ArrayUtil.contains(environments, profile)).count();
        return count > 0;
    }

    /**
     * 创建FeignBuilderHelper，feign调用时执行
     */
    @Bean
    public FeignBuilderHelper feignBuilderHelper() {
        final boolean isolation = needIsolation();
        return new FeignBuilderHelper(isolation);
    }

    /**
     * 创建JedisTools
     */
    @Bean
    public JedisTools jedisTools() {
        final boolean isolation = needIsolation();
        if (!isolation) {
            return null;
        }

        final String redisUrlPath = FeignIsolationConfiguration.redisUrlPath();
        final String redisPortPath = FeignIsolationConfiguration.redisPortPath();
        final String redisUserPath = FeignIsolationConfiguration.redisUserPath();
        final String redisPwdPath = FeignIsolationConfiguration.redisPwdPath();

        Assert.notBlank(redisUrlPath, "redis url path can not be null.");
        Assert.notBlank(redisPortPath, "redis url port can not be null.");
        Assert.notBlank(redisPwdPath, "redis url password can not be null.");

        final String redisUrl = environment.getProperty(redisUrlPath);
        final int redisPort = Integer.parseInt(Objects.requireNonNull(environment.getProperty(redisPortPath)));
        final String redisUser = environment.getProperty(redisUserPath);
        final String redisPassword = environment.getProperty(redisPwdPath);

        return new JedisTools(redisUrl, redisPort, redisUser, redisPassword);
    }

    @Override
    public void run(ApplicationArguments args) {
        final boolean isolation = needIsolation();

        if (!isolation) {
            log.info("Feign isolation startup failed,Environmental mismatch ~~~");
            return;
        }

        String defaultIp = FeignIsolationConfiguration.defaultIp();
        String localIpAddr = NetworkUtil.getLocalIpAddr();
        if (defaultIp.equals(localIpAddr)) {
            return;
        }

        heartbeatRegistration();

        log.info("Feign isolation startup successful ~~~");
    }

    @Resource
    private JedisTools jedisTools;

    private void heartbeatRegistration() {
        String springApplicationName = environment.getProperty(FeignIsolationConstants.SPRING_APPLICATION_NAME);
        SleuthThreadScheduledPool.scheduleWithFixedDelay(() -> {
            jedisTools.set(FEIGN_REDIS_KEY_PREFIX + springApplicationName, springApplicationName, 5);
            // log.info("Feign isolation heartbeat...");
        }, 1, 4, TimeUnit.SECONDS);
    }

}