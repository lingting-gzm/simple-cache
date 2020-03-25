package com.hccake.simpleredis.type.multistring;

import com.hccake.simpleredis.config.SimpleCacheConfig;
import com.hccake.simpleredis.core.CacheOps;
import com.hccake.simpleredis.core.KeyGenerator;
import com.hccake.simpleredis.core.OpType;
import com.hccake.simpleredis.template.function.ResultMethod;
import com.hccake.simpleredis.template.function.VoidMethod;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Hccake
 * @version 1.0
 * @date 2019/9/2 15:33
 */
public class OpsForMultiString extends CacheOps {

    /**
     * 生成并返回一个 OpsForMultiString 对象
     *
     * @param cacheForString 注解
     * @param keyGenerator   key 生成方法
     * @param pointMethod    织入方法
     * @param returnType     返回类型
     * @param redisTemplate    缓存工具类
     * @param multiByItem
     */
    public OpsForMultiString(CacheForMultiString cacheForString, KeyGenerator keyGenerator, ResultMethod<Object> pointMethod, Type returnType, StringRedisTemplate redisTemplate, Collection<String> multiByItem) throws UnsupportedEncodingException {

        super(redisTemplate, pointMethod, returnType);

        //缓存key
        List<String> keys = keyGenerator.getKeys(cacheForString.key(), cacheForString.keyJoint(), multiByItem);

        //缓存操作类型
        OpType opType = cacheForString.type();

        ValueOperations<String, String> operations = redisTemplate.opsForValue();

        //CACHED缓存需要的属性
        if (OpType.CACHED.equals(opType)) {
            //redis 分布式锁的 key
            String keyPrefix = cacheForString.key() + cacheForString.keyJoint() + multiByItem.stream().collect(Collectors.joining());
            keyPrefix = DigestUtils.md5DigestAsHex(keyPrefix.getBytes("UTF-8"));
            String lockKey = keyPrefix + SimpleCacheConfig.lockKeySuffix();
            this.setLockKey(lockKey);

            //缓存查询操作
            Supplier cacheQuery = () -> operations.multiGet(keys);
            this.setCacheQuery(cacheQuery);

            //缓存更新操作  Cached 中 更新的缓存数据可能只有少数几个
            Consumer<Object> cachePut = (Object obj) -> {
                Map<Integer, String> values = (Map<Integer, String>)obj;

                Map<String, String> map = new HashMap<>();
                for (Map.Entry<Integer, String> entry : values.entrySet()) {
                    map.put(keys.get(entry.getKey()), entry.getValue());
                }
                operations.multiSet(map);
            };
            this.setCachePut(cachePut);

        }

        // PUT 操作需要的属性
        if (OpType.PUT.equals(opType)) {

            //PUT 操作将全量数据 全部重新缓存
            Consumer<Object> cachePut = (Object obj) -> {
                List<String> values = (List<String>)obj;

                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < keys.size(); i++) {
                    map.put(keys.get(i), values.get(i));
                }
                operations.multiSet(map);
            };
            this.setCachePut(cachePut);
        }


        // DEL 操作需要的属性
        if (OpType.DEL.equals(opType)) {
            VoidMethod cacheDel = () -> {
                for (String key : keys) {
                    redisTemplate.delete(key);
                }
            };
            this.setCacheDel(cacheDel);
        }

    }

}