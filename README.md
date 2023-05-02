# FeignIsolation

> 用于服务注册到nacos时隔离，且feignRequest调用时隔离

## 注解类：FeignIsolation

- environments(String[]:default dev)：该注解生效环境，默认dev
- defaultIp：默认服务IP
- isolationIps：需要强制隔离服务的IPS
- skipIsolationServices：允许跳过隔离限制的服务（也可使用@FeignProvider指定去调用默认服务节点）
- serviceSign：指定隔离的服务名标识 请求的service服务名中包含该标识才进行隔离

## 配置类：FeignIsolationConfiguration

## 核心类：FeignIsolationCore、FeignBuilderHelper

# FeignProxy

> 用于feignRequest执行时重定向请求地址

## 注解类：FeignProvider

- value(String)：调用服务的后缀名
- environments(String[]:default dev)：该注解生效环境，默认dev

## 核心类：FeignBeanPostProcessor


