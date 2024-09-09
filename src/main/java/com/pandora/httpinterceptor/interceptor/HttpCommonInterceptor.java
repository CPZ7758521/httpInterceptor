package com.pandora.httpinterceptor.interceptor;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import entity.SysOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Component
@Aspect
public class HttpCommonInterceptor {
    private static Logger LOG = LoggerFactory.getLogger(HttpCommonInterceptor.class);

    /**
     * 拦截所有http请求，获取ldap信息
     * 1 包名
     * 2 类名
     * 3 方法名
     * 4 参数
     */
    @Before("execution(* com.pandora..*.controller..*.*(..))")
    public void handleControllerMethod(JoinPoint jp) {
        //原始的HTTP请求和响应的信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = attributes.getRequest();
        Signature signature = jp.getSignature();

        MethodSignature targetMethod = (MethodSignature) signature;
        String ldap = request.getHeader("X-Context-Ldap");
        String url = request.getRequestURL().toString();
        String userAgent = request.getHeader("User-Agent");
        String httpMethod = request.getMethod();
        String methodName = targetMethod.getName();
        Object[] args = jp.getArgs();

        SysOperationLog sysOperationLog = new SysOperationLog();
        sysOperationLog.setUserAgent(userAgent);
        sysOperationLog.setReqUrl(url);
        sysOperationLog.setUserId(ldap);
        sysOperationLog.setMethod(methodName);
        sysOperationLog.setHttpMethod(httpMethod);

        //序列化保持定义顺序  &= 按位与  ~ 取反，这样计算结果，对排序特性位计算为0，禁用了排序保持了定义时的顺序 JSON.DEFAULT_GENERATE_FEATURE全局生效
        JSON.DEFAULT_GENERATE_FEATURE &= ~SerializerFeature.SortField.getMask();

        //序列化时基于字段 填true ，序列化时基于 getter 填false
        SerializeConfig serializeConfig = new SerializeConfig(true);
        if (Objects.nonNull(args)) {
            sysOperationLog.setParamData(JSON.toJSONString(args, serializeConfig));
        }

        LOG.info("request log >>> {}", JSON.toJSONString(args, serializeConfig));
    }

}
