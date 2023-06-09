package io.github.hyuga0410.feign.proxy;

import cn.hyugatool.core.collection.ArrayUtil;
import cn.hyugatool.core.instance.ReflectionUtil;
import cn.hyugatool.core.object.ObjectUtil;
import cn.hyugatool.core.string.StringUtil;
import feign.InvocationHandlerFactory;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FeignBeanPostProcessor
 * <p>
 * BeanPostProcessor:允许自定义修改新bean实例的工厂钩子，例如检查标记接口或用代理包装它们。
 * ApplicationContexts可以在它们的bean定义中自动检测BeanPostProcessor bean，并将它们应用于随后创建的任何bean。
 * 普通bean工厂允许对后处理器进行程序化注册，并应用于通过该工厂创建的所有bean。
 *
 * @author pengqinglong
 * @since 2022/4/9
 */
@Slf4j
@Component
public class FeignBeanPostProcessor implements ApplicationContextAware, BeanPostProcessor, PriorityOrdered {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 将此{@code BeanPostProcessor}应用于给定的新bean实例
     * <p>
     * 在任何bean初始化回调之前（如InitializingBean的{@code afterPropertiesSet}或自定义init方法）。Bean将已经填充属性值。
     * <p>
     * 返回的bean实例可能是围绕原始实例的包装器。
     * <p>
     * 默认实现按原样返回给定的{@code bean}。
     *
     * @param bean     新的bean实例
     * @param beanName bean名称
     * @return 要使用的bean实例，无论是原始实例还是包装实例；
     * 如果{@code null}，则不会调用后续的BeanPostProcessors
     * @throws org.springframework.beans.BeansException in case of errors
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
     */
    @Nullable
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        try {
            // 用于管理注入元数据的内部类。不打算在应用程序中直接使用
            InjectionMetadata resourceMetadata = this.buildResourceMetadata(bean.getClass());
            resourceMetadata.inject(bean, beanName, null);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return bean;
    }

    /**
     * 构建资源元数据，自定义要对Class做的定制化处理逻辑
     *
     * @param clazz Class
     * @return InjectionMetadata 注入元数据
     */
    private InjectionMetadata buildResourceMetadata(final Class<?> clazz) {
        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        Class<?> targetClass = clazz;

        do {

            final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();
            // springCore方法循环获取class类字段属性[对给定类中所有局部声明的字段调用给定的回调。]
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                // 判断class类中所有字段
                if (field.isAnnotationPresent(Resource.class)) {
                    // 仅处理带@Resource注解的类属性成员
                    currElements.add(new ResourceElement(field, null));
                }
            });

            elements.addAll(0, currElements);
            targetClass = targetClass.getSuperclass();

        } while (targetClass != null && targetClass != Object.class);

        return new InjectionMetadata(clazz, elements);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 2;
    }

    private class ResourceElement extends InjectionMetadata.InjectedElement {

        private static final String H = "h";
        private static final String TARGET = "target";
        private static final String ADVISED = "advised";
        private static final String DISPATCH = "dispatch";
        private static final String TARGET_SOURCE = "targetSource";

        public ResourceElement(Member member, @Nullable PropertyDescriptor pd) {
            super(member, pd);
        }

        @Override
        protected void inject(@NonNull Object target, @Nullable String requestingBeanName, PropertyValues pvs)
                throws Throwable {

            Field field = (Field) this.member;

            ReflectionUtils.makeAccessible(field);

            // 获取注解
            FeignProvider annotation = field.getAnnotation(FeignProvider.class);
            if (annotation == null) {
                return;
            }

            String provider = annotation.value();

            if (StringUtil.isEmpty(provider)) {
                return;
            }

            String[] environments = annotation.environments();
            String environmentOfApplication = applicationContext.getBean(Environment.class)
                    .getProperty(FeignProxyConstants.SPRING_CONFIG_ACTIVATE_ON_PROFILE);

            if (!ArrayUtil.contains(environments, environmentOfApplication)) {
                return;
            }

            // 获取spring代理类
            Object springObj = field.get(target);
            if (springObj == null) {
                return;
            }

            // 解刨代理类 找到最终的源hardCodedTarget
            Object springProxy = ReflectionUtil.getFieldValue(springObj, H);
            Object advised = ReflectionUtil.getFieldValue(springProxy, ADVISED);
            Object targetSource = ReflectionUtil.getFieldValue(advised, TARGET_SOURCE);
            Object feignTarget = ReflectionUtil.getFieldValue(targetSource, TARGET);
            Object feignSource = ReflectionUtil.getFieldValue(feignTarget, H);
            Object hardCodedTarget = ReflectionUtil.getFieldValue(feignSource, TARGET);
            if (!(hardCodedTarget instanceof Target.HardCodedTarget)) {
                return;
            }
            if (hardCodedTarget instanceof FeignProxy) {
                return;
            }

            // 代理hardCodedTarget
            Target.HardCodedTarget<Object> feignTargetSource = ObjectUtil.cast(hardCodedTarget);
            FeignProxy<Object> feignProxy = new FeignProxy<>(feignTargetSource, annotation.value());
            ReflectionUtil.setFieldValue(feignSource, TARGET, feignProxy);

            // 代理每个方法实现
            Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = ObjectUtil.cast(ReflectionUtil.getFieldValue(feignSource, DISPATCH));
            for (InvocationHandlerFactory.MethodHandler handler : dispatch.values()) {
                ReflectionUtil.setFieldValue(handler, TARGET, feignProxy);
            }
        }
    }

}