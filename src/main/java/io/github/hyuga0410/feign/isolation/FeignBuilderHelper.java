package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.collection.ListUtil;
import cn.hyugatool.core.number.NumberUtil;
import cn.hyugatool.core.string.StringUtil;
import cn.hyugatool.core.uri.URLUtil;
import cn.hyugatool.system.NetworkUtil;
import cn.hyugatool.system.SystemUtil;
import feign.Feign;
import feign.Target;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;

import static io.github.hyuga0410.feign.isolation.FeignIsolationConstants.FEIGN_REDIS_KEY_PREFIX;

/**
 * FeignBuilderHelper
 *
 * @author hyuga
 * @since 2022/01/07
 */
@Slf4j
public class FeignBuilderHelper extends Feign.Builder {

    /**
     * 是否需要隔离
     */
    private final boolean isolation;
    /**
     * 服务IP
     */
    private final String localIpAddr = NetworkUtil.getLocalIpAddr();
    /**
     * 默认环境IP
     */
    private final String defaultIp = FeignIsolationConfiguration.defaultIp();
    /**
     * 强制隔离IPS
     */
    private final List<String> isolationIps = FeignIsolationConfiguration.isolationIps();
    /**
     * 需要跳过隔离的服务
     */
    private final List<String> skipIsolationServices = FeignIsolationConfiguration.skipIsolationServices();
    /**
     * 隔离服务标识
     */
    private final String serviceSign = FeignIsolationConfiguration.serviceSign();
    /**
     * 服务器名
     */
    private final String hostName = SystemUtil.getLocalHostName();
    /**
     * 数值IP
     */
    private final BigDecimal ipNumber = NumberUtil.getNumber(localIpAddr);
    /**
     * 自定义后缀
     */
    private final String serviceIsolationSuffix = String.format("%s-%s", ipNumber, hostName);

    public FeignBuilderHelper(boolean isolation) {
        super();
        this.isolation = isolation;
    }

    /**
     * index1:uri
     * index2:suffix
     * index3:path
     */
    private static final String DYNAMIC_URL = "%s" + FeignIsolationConstants.ISOLATION_SYMBOL + "%s%s";

    @Resource
    private JedisTools jedisTools;

    /**
     * 发起feign请求时触发
     *
     * @param target Feign#Target
     * @return T
     */
    @Override
    public <T> T target(Target<T> target) {
        return super.target(new Target.HardCodedTarget<>(target.type(), target.name(), target.url()) {
            @Override
            public String url() {
                if (!isolation) {
                    // 环境未命中，不启用隔离
                    return super.url();
                }

                final String urlStr = super.url();
                final URL url = URLUtil.url(urlStr);
                final String host = url.getHost();
                final String uri = URLUtil.getHost(url).toString();
                final String path = url.getPath();

                if (StringUtil.equals(defaultIp, localIpAddr)) {
                    // 当前IP地址为默认服务IP地址，无需隔离
                    return super.url();
                }

                if (!urlStr.contains(serviceSign)) {
                    // 请求url不是指定的服务标识不隔离
                    return super.url();
                }

                // 符合强制隔离IP要求
                if (ListUtil.anyMatch(isolationIps, isolationIp -> isolationIp.equals(localIpAddr))) {
                    if (ListUtil.anyMatch(skipIsolationServices, skipIsolationService -> skipIsolationService.equals(uri))) {
                        // 需要强制隔离的ip，但同时又配置在了skipIsolationServices中
                        return super.url();
                    } else {
                        // 需要强制隔离的ip
                        return String.format(DYNAMIC_URL, uri, serviceIsolationSuffix, path);
                    }
                }

                String key = FEIGN_REDIS_KEY_PREFIX + host + FeignIsolationConstants.ISOLATION_SYMBOL + serviceIsolationSuffix;
                String applicationName = jedisTools.get(key);
                if (StringUtil.hasText(applicationName)) {
                    return String.format(DYNAMIC_URL, uri, serviceIsolationSuffix, path);
                } else {
                    return super.url();
                }
            }
        });
    }

}
