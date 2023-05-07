# FeignIsolation

> 用于`SpringBoot`服务注册到`nacos`时听过追加服务名后缀进行服务隔离
>
> 以及`feignRequest`调用时优先调用同属IP服务，不存在同属IP服务，再调用默认IP的服务。

## 注解类：FeignIsolation

- environments(String[]:default dev)：指定生效环境，默认dev
- defaultIp：默认服务IP
- isolationIps：需要强制隔离服务的IPS
- skipIsolationServices：允许跳过隔离限制的服务（也可使用@FeignProvider指定去调用默认服务节点）
- serviceSign：指定隔离的服务名标识 请求的service服务名中包含该标识才进行隔离

## 配置类：FeignIsolationConfiguration

## 核心类：FeignIsolationCore、FeignBuilderHelper

## DEMO

__启动类添加注解__

```java
@FeignIsolation(serviceSign = "-hyuga-", defaultIp = "127.0.0.1", isolationIps = "127.0.0.2#127.0.0.3", skipIsolationServices = "service-hyuga-dict")
```

__`application.yml`或`bootstrap.yml`添加`${feign-isolation-suffix}`__

```yaml
spring:
  application:
    name: service-hyuga-dict${feign-isolation-suffix}
```

# FeignProxy

> 用于`feignRequest`执行时重定向请求地址

## 注解类：FeignProvider

- value(String)：调用服务的后缀名
- environments(String[]:default dev)：指定生效环境，默认dev

## 核心类：FeignBeanPostProcessor

## DEMO

```java
import javax.annotation.Resource;

@Resource
@FeignProvider(value = "hyuga")
private HyugaDictFeign hyugaDictFeign;
```


