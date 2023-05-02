package io.github.hyuga0410.feign.isolation;

import cn.hyugatool.core.collection.ListUtil;
import cn.hyugatool.core.number.NumberUtil;
import cn.hyugatool.core.string.StringUtil;
import cn.hyugatool.core.uri.URLUtil;
import cn.hyugatool.system.NetworkUtil;
import cn.hyugatool.system.SystemUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import feign.Feign;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * FeignBuilderHelper
 *
 * @author hyuga
 * @since 2022/01/07
 */
@Slf4j
public class FeignBuilderHelper extends Feign.Builder {

    public static final String NACOS_SERVICES = "NACOS_SERVICES";

    RemovalListener<String, List<String>> nacosServicesRemovalListener =
            notification -> log.info("nacos service cache is lose effectiveness.");
    private final LoadingCache<String, List<String>> cacheMap = CacheBuilder.newBuilder()
            // 初始容量
            .initialCapacity(1)
            // 并发级别
            .concurrencyLevel(10)
            // 缓存多久过期
            .expireAfterAccess(Duration.ofSeconds(10))
            .removalListener(nacosServicesRemovalListener)
            .build(new CacheLoader<>() {
                @Override
                public List<String> load(@NonNull String key) throws NacosException {
                    List<String> nacosAllServices = getNacosAllServices();
                    log.info("nacos service cache is initialization completed.");
                    return nacosAllServices;
                }
            });

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
    private static final String DYNAMIC_URL = "%s-%s%s";

    @Resource
    private ApplicationContext applicationContext;

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
                    // 无需隔离
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

                List<String> servicesByKeyword = getNacosServicesByHost(host);
                if (ListUtil.isEmpty(servicesByKeyword)) {
                    throw new RuntimeException(String.format("[%s]服务不存在，请检查!", host));
                }
                if (servicesByKeyword.size() > 1) {
                    long localService = servicesByKeyword.stream().filter(serviceName -> serviceName.contains(serviceIsolationSuffix)).count();
                    if (localService == 1) {
                        // 存在同类服务节点时调用
                        return String.format(DYNAMIC_URL, uri, serviceIsolationSuffix, path);
                    }
                }
                return super.url();
            }
        });
    }

    public List<String> getNacosServicesByHost(String host) {
        try {
            List<String> nacosServices = cacheMap.get(NACOS_SERVICES);
            return nacosServices.stream().filter(server -> server.startsWith(host)).collect(Collectors.toList());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getNacosAllServices() throws NacosException {
        NacosDiscoveryProperties nacosDiscoveryProperties = applicationContext.getBean(NacosDiscoveryProperties.class);
        NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();
        ListView<String> servicesOfServer = namingService.getServicesOfServer(1, Integer.MAX_VALUE, nacosDiscoveryProperties.getGroup());
        return servicesOfServer.getData();
    }

}
