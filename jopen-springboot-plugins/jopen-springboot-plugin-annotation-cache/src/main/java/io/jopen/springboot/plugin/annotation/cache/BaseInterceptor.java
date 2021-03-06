package io.jopen.springboot.plugin.annotation.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * {@link HandlerMethod}
 * 通用方法抽象
 * <p>
 * {@link HandlerInterceptor}
 * {@link Method}
 *
 * @author maxuefeng
 * {@link Annotation}
 * 注解实例调用<code>getClass()<code/>方法的结果是一个Proxy对象 具体打印结果是com.sun.proxy.$Proxy72
 * 注解实例调用<code>annotationType()<code/>方法的结果是一个正确的Class对象  而非一个Proxy对象
 * @see MapMaker#weakValues()
 * @see MapMaker#weakKeys()
 * @see Annotation#annotationType()
 */
// @Component
public class BaseInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseInterceptor.class);

    //
    private final static Cache<Integer, Set<Annotation>> GUAVA_ANNOTATION_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build();


    /**
     * 缓存接口的注解，此处对数据有强一致性的要求，读远大于写
     * read >> write
     *
     * @see ClassToInstanceMap
     * @see java.util.concurrent.ConcurrentHashMap
     */
    @Deprecated
    private final static Map<Integer, ClassToInstanceMap<Annotation>> ANNOTATION_CACHE = new HashMap<>(300);

    /**
     * {@link MapMaker}
     * GC Key and Value
     * if HashMap  it will gc value，key not gc
     */
    private final static ConcurrentMap<Integer, Set<Annotation>> CACHE = new MapMaker().weakValues().makeMap();

    /**
     * 获取指定标记
     *
     * @param type    注解类型
     * @param handler 目标接口方法
     * @return 返回指定的注解实例
     */
    @Nullable
    @Deprecated
    public <TYPE extends Annotation> TYPE getMark(@NonNull Class<TYPE> type,
                                                  @NonNull Object handler) {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        int key = handlerMethod.getMethod().toGenericString().hashCode();
        ClassToInstanceMap<Annotation> classToInstanceMap = ANNOTATION_CACHE.get(key);
        return classToInstanceMap == null ? null : classToInstanceMap.getInstance(type);
    }

    @Nullable
    public <TYPE extends Annotation> TYPE getApiServiceAnnotation(@NonNull Class<TYPE> type, @NonNull Object handler) {
        return Optional.of(handler)
                .filter(h -> h instanceof HandlerMethod)
                .map(h -> (HandlerMethod) h)
                .map(h -> {
                    TYPE annotation = h.getMethodAnnotation(type);
                    return Optional.ofNullable(annotation).orElse(h.getBeanType().getDeclaredAnnotation(type));
                })
                .orElse(null);
    }

    /**
     * 基本缓存
     *
     * @param type
     * @param handler
     * @param <TYPE>
     * @return
     * @throws ExecutionException
     */
    @Nullable
    public <TYPE extends Annotation> TYPE getApiServiceAnnotation1(@NonNull Class<TYPE> type, @NonNull Object handler) throws ExecutionException {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        int hashCode = handlerMethod.hashCode();
        Set<Annotation> annotations = GUAVA_ANNOTATION_CACHE.get(hashCode, () -> {
            HashSet<Annotation> hashSet = Sets.newHashSet();
            TYPE methodAnnotation = handlerMethod.getMethodAnnotation(type);
            hashSet.add(methodAnnotation);
            return hashSet;
        });
        // 加载缓存
        Optional<Annotation> optional = annotations.parallelStream()
                .filter(annotation -> annotation.annotationType().equals(type))
                .findFirst();
        return (TYPE) optional.orElse(null);
    }


    /**
     * {@link org.springframework.boot.CommandLineRunner} start spring application {@link BaseInterceptor#runCompleteScanner(String...)}
     *
     * @param args
     * @throws Exception
     */
    public void runCompleteScanner(String... args) throws Exception {
        LOGGER.info("load api interface annotation");

        // TODO  需要设置controller包  API访问策略
        // List<Class<?>> classList = ReflectUtil.getClasses("com.planet.biz.modules.controller");
        List<Class<?>> classList = new ArrayList<>();

        // 需要过滤的注解
        final Set<Class<?>> filterTypes = ImmutableSet.of(
                RestController.class,
                Controller.class,
                ResponseBody.class,
                Component.class,
                RequestMapping.class,
                PostMapping.class,
                GetMapping.class,
                PostConstruct.class);

        classList.parallelStream()
                // 进行过滤
                .filter(controllerType -> controllerType.getDeclaredAnnotation(Controller.class) != null || controllerType.getDeclaredAnnotation(RestController.class) != null)
                // 进行消费
                .forEach(controllerType -> {
                    // 类级别的注解
                    Annotation[] typeAnnotations = controllerType.getDeclaredAnnotations();

                    Method[] methods = controllerType.getDeclaredMethods();
                    for (Method method : methods) {
                        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
                        Set<Annotation> mergeSet = new HashSet<>();
                        int key = method.toGenericString().hashCode();

                        // 添加类级别的注解
                        if (typeAnnotations != null) {
                            Collections.addAll(mergeSet, typeAnnotations);
                        }

                        // 添加方法级别的注解
                        if (methodAnnotations != null) {
                            Collections.addAll(mergeSet, methodAnnotations);
                        }

                        MutableClassToInstanceMap<Annotation> classToInstanceMap = MutableClassToInstanceMap.create();
                        for (Annotation annotation : mergeSet) {
                            if (filterTypes.contains(annotation.annotationType())) {
                                continue;
                            }
                            classToInstanceMap.put(annotation.annotationType(), annotation);
                        }
                        ANNOTATION_CACHE.put(key, classToInstanceMap);
                    }
                });

        LOGGER.info("cache api interface annotation complete");
    }

}
