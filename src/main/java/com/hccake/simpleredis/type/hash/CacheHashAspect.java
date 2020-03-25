package com.hccake.simpleredis.type.hash;

import com.hccake.simpleredis.core.CacheOps;
import com.hccake.simpleredis.core.KeyGenerator;
import com.hccake.simpleredis.template.function.ResultMethod;
import com.hccake.simpleredis.template.TemplateMethod;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Hccake
 * @version 1.0
 * @date 2019/8/31 18:01
 */
@Aspect
@Component
public class CacheHashAspect {
    Logger log = LoggerFactory.getLogger(CacheHashAspect.class);
    /**
     * 模板方法
     */
    @Resource(name = "normalTemplateMethod")
    private TemplateMethod templateMethod;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Pointcut("@annotation(com.hccake.simpleredis.type.hash.CacheForHash)")
    public void pointCut() {
    }


    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {


        //获取目标方法
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        log.debug("=======The hash cache aop is executed! method : {}", method.getName());

        //方法返回值
        Type returnType = method.getGenericReturnType();

        //根据方法的参数 以及当前类对象获得 keyGenerator
        Object target = point.getTarget();
        Object[] arguments = point.getArgs();
        KeyGenerator keyGenerator = new KeyGenerator(target, method, arguments);

        // 织入方法
        ResultMethod<Object> pointMethod = CacheOps.genPointMethodByPoint(point);


        //获取注解对象
        CacheForHash cacheForHash = AnnotationUtils.getAnnotation(method, CacheForHash.class);

        //获取操作类
        CacheOps ops = new OpsForHash(cacheForHash, keyGenerator, pointMethod, returnType, redisTemplate);

        //执行对应模板方法
        return templateMethod.runByOpType(ops, cacheForHash.type());

    }


}