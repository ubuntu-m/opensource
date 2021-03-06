package io.jopen.springboot.plugin.limit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 无副作用。
 * 没有有害的随机性。
 * 对于同样的输入参数和数据集，总是产生相同的写入命令   比如math.random()
 * <p>
 * <p>
 *  1、Lua脚本可以在redis单机模式、主从模式、Sentinel集群模式下正常使用，但是无法在分片集群模式下使用。（脚本操作的key可能不在同一个分片）
 *  2、Lua脚本中尽量避免使用循环操作（可能引发死循环问题），尽量避免长时间运行。
 *  3、redis在执行lua脚本时，默认最长运行时间时5秒，当脚本运行时间超过这一限制后，Redis将开始接受其他命令但不会执行
 * （以确保脚本的原子性，因为此时脚本并没有被终止），而是会返回“BUSY”错误。
 * <p>
 * 如果想读取{@link Limiting} 中的参数数据 需要实现接口{@link ImportAware#setImportMetadata(AnnotationMetadata)}
 *
 * @author maxuefeng
 */
@Component
@Configuration
public class LimitPluginConfiguration implements WebMvcConfigurer, ImportAware {

    private FlowControlInterceptor flowControlInterceptor;

    @Autowired
    public LimitPluginConfiguration(FlowControlInterceptor flowControlInterceptor) {
        this.flowControlInterceptor = flowControlInterceptor;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(flowControlInterceptor)
                .order(this.flowControlInterceptor.getOrder())
                .addPathPatterns(this.flowControlInterceptor.getPathPatterns())
                .excludePathPatterns(this.flowControlInterceptor.getExcludePathPatterns());

    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        AnnotationAttributes enableLimit = AnnotationAttributes
                .fromMap(importMetadata.getAnnotationAttributes(EnableJopenLimit.class.getName(), false));

        if (enableLimit == null) {
            throw new IllegalArgumentException(
                    "@EnableLimit is not present on importing class " + importMetadata.getClassName());
        }
        // 目标类的Class
        Class<? extends LimitKeyProducer> limitKeyFunctionType = enableLimit.getClass("limitKeyFunctionType");
        try {
            LimitKeyProducer instance = limitKeyFunctionType.newInstance();
            this.flowControlInterceptor.setLimitKeyProducer(instance);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            this.flowControlInterceptor.setLimitKeyProducer(new LimitKeyProducer.IPLimitKeyStrategy());
        }

        // 拦截路径
        String[] pathPatterns = enableLimit.getStringArray("pathPatterns");
        // 不包含路径
        String[] excludePathPatterns = enableLimit.getStringArray("excludePathPattern");
        // 拦截器顺序
        int order = enableLimit.getNumber("order");
        // 是否开启黑名单策略
        boolean enablePullBlack = enableLimit.getBoolean("enablePullBlack");
        // 拉黑逻辑处理策略
        Class<? extends Keeper> limitKeeperType = enableLimit.getClass("limitKeeperType");
        
        this.flowControlInterceptor.setEnablePullBlack(enablePullBlack);
        this.flowControlInterceptor.setLimitKeeperType(limitKeeperType);
        this.flowControlInterceptor.setPathPatterns(pathPatterns);
        this.flowControlInterceptor.setExcludePathPatterns(excludePathPatterns);
        this.flowControlInterceptor.setOrder(order);
    }
}
