# FeignIsolation

> 用于`SpringBoot`服务注册到`nacos`时通过追加服务名后缀进行服务隔离。
>
> 以及`feignRequest`调用时优先调用同属IP服务，不存在同属IP服务，再调用默认IP的服务。
>
> 所有使用该组件的服务均须提供redis配置。

## 注解类：FeignIsolation

- environments(String[]:default dev)：指定生效环境，默认dev
- defaultIp：默认服务IP
- isolationIps：需要强制隔离服务的IPS
- skipIsolationServices：允许跳过隔离限制的服务（也可使用@FeignProvider指定去调用默认服务节点）
- serviceSign：指定隔离的服务名标识 请求的service服务名中包含该标识才进行隔离
- redisUrlPath：Redis地址配置路径
- redisPortPath：Redis端口配置路径
- redisUserPath：Redis用户配置路径
- redisPwdPath：Redis密码配置路径

## 配置类

### FeignIsolationConfiguration

> @FeignIsolation注解相关配置信息读取，并注入系统变量`feign-isolation-suffix`值。

## 核心类

### FeignIsolationCore

> 注入`FeignBuilderHelper`和`JedisTools`，以及服务名心跳注册机制。

### FeignBuilderHelper

> `FeignBuilderHelper`实现feign请求动态调用。

## DEMO

### __`application.yml`或`bootstrap.yml`添加`${feign-isolation-suffix}`__

```yaml
spring:
  application:
    name: service-hyuga-dict${feign-isolation-suffix}
```

### __启动类添加注解__

```java
@FeignIsolation(serviceSign = "-hyuga-", defaultIp = "127.0.0.1", isolationIps = "127.0.0.2#127.0.0.3", skipIsolationServices = "service-hyuga-dict")
```

# FeignProxy

> 用于`feignRequest`执行时重定向请求地址

## 注解类

### FeignProvider

- value(String)：调用服务的后缀名
- environments(String[]:default dev)：指定生效环境，默认dev

## 核心类

### FeignBeanPostProcessor

> 在任何bean初始化回调之前，扫描含`@Resource`和`@FeignProvider`的成员属性进行解刨处理，以实现feign请求动态调用。

## DEMO

```java
import javax.annotation.Resource;

@Resource
@FeignProvider(value = ".hyuga")
private HyugaDictFeign hyugaDictFeign;
```


