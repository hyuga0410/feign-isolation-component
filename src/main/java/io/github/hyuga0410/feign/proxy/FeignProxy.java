package io.github.hyuga0410.feign.proxy;

import cn.hyugatool.core.string.StringUtil;
import cn.hyugatool.core.uri.URLUtil;
import feign.Target;
import lombok.SneakyThrows;

import java.net.URL;

/**
 * FeignProxy
 *
 * @author pengqinglong
 * @since 2022/4/11
 */
public class FeignProxy<T> extends Target.HardCodedTarget<T> {

    /**
     * 默认请求路径
     */
    private static final String S_S = "%s://%s%s";
    /**
     * 带自定义后缀请求路径
     */
    private static final String S_SS = "%s://%s-%s%s";
    /**
     * FeignHardCodedTarget
     */
    private final HardCodedTarget<T> target;
    /**
     * URL请求后缀
     */
    private final String suffix;

    public FeignProxy(HardCodedTarget<T> target, String suffix) {
        super(target.type(), target.url());
        // feign代理
        this.target = target;
        this.suffix = suffix;
    }

    @SneakyThrows
    @Override
    public String url() {
        // 请求URL
        final String urlStr = target.url();
        final URL url = URLUtil.url(urlStr);
        final String path = url.getPath();
        final String protocol = new URL(urlStr).getProtocol();

        return StringUtil.isEmpty(suffix) ?
                // suffix为空则为默认环境，正常发起请求
                String.format(S_S, protocol, target.name(), path) :
                // 带后缀，且URL不为空
                String.format(S_SS, protocol, target.name(), suffix, path);
    }

}