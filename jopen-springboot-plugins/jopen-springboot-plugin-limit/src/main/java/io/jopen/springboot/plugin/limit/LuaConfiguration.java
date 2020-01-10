package io.jopen.springboot.plugin.limit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

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
 *
 * @author maxuefeng
 */
@Configuration
public class LuaConfiguration {

    private String limitLuaScript = "local key = \"rate.limit:\" .. KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local current = tonumber(redis.call('get', key) or \"0\")\n" +
            "if current + 1 > limit then\n" +
            "    return 0\n" +
            "else\n" +
            "    redis.call(\"INCRBY\", key, \"1\")\n" +
            "    redis.call(\"expire\", key, ARGV[2])\n" +
            "    return current + 1\n" +
            "end";

    /**
     * 加载Lua限流脚本
     *
     * @return 脚本包装对象
     */
    @Bean(name = "limitScript")
    public DefaultRedisScript<Number> limitScript() {

        DefaultRedisScript<Number> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Number.class);
        // redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/limiting.lua")));
        redisScript.setScriptText(limitLuaScript);

        return redisScript;
    }
}