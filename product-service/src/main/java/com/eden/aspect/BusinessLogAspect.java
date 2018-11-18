package com.eden.aspect;

import com.alibaba.fastjson.JSON;
import com.eden.enums.ResultEnum;
import com.eden.mapper.TServiceLogMapper;
import com.eden.model.TServiceLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Aspect
@Component
public class BusinessLogAspect {

    @Autowired
    private TServiceLogMapper serviceLogMapper;

    @Pointcut("@annotation(com.eden.annotation.BusinessLog)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        TServiceLog serviceLog = doBefore(joinPoint);

        Object returnValue;
        try {
            returnValue = joinPoint.proceed();
        } catch (Throwable throwable) {
            doThrowing(serviceLog, throwable);
            // 需对外抛出异常，否则其他切面无法获取到
            throw throwable;
        }

        doAfter(serviceLog, returnValue);

        // 需将结果返回，否则会导致目标结果为空
        return returnValue;
    }

    private void doAfter(TServiceLog serviceLog, Object returnValue) {
        serviceLog.setOutParam(JSON.toJSONString(returnValue));
        serviceLog.setStatus(new Byte(String.valueOf(ResultEnum.SUCCESS.getCode())));
        serviceLogMapper.updateByPrimaryKey(serviceLog);
    }

    private void doThrowing(TServiceLog serviceLog, Throwable throwable) {
        serviceLog.setStatus(new Byte(String.valueOf(ResultEnum.TRADE_ERROR.getCode())));
        serviceLog.setOutParam(throwable.getMessage());
        serviceLogMapper.updateByPrimaryKey(serviceLog);
    }

    private TServiceLog doBefore(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        // 请求URL
        String url = request.getRequestURL().toString();
        // 请求IP
        String ip = request.getRemoteHost();

        // 请求参数
        Object[] args = joinPoint.getArgs();
        StringBuilder params = new StringBuilder();
        for (Object arg : args) {
            params.append(JSON.toJSONString(arg)).append("\n\n");
        }

        // 服务类名、方法名
        Signature signature = joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();

        TServiceLog serviceLog = new TServiceLog();
        serviceLog.setCreateTime(new Date());
        serviceLog.setIp(ip);
        serviceLog.setUrl(url);
        serviceLog.setClassName(className);
        serviceLog.setMethodName(methodName);
        serviceLog.setInParam(params.toString());

        // 需设置mybatis 将自增主键ID设置进实体中 (useGeneratedKeys="true" keyProperty="id")
        serviceLogMapper.insert(serviceLog);
        return serviceLog;
    }
}
