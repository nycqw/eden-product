package com.eden.aspect;

import com.eden.annotation.FlowNode;
import com.eden.mapper.TFlowNodeMapper;
import com.eden.model.TFlowNode;
import com.eden.util.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author chenqw
 * @version 1.0
 * @since 2018/11/14
 */
@Aspect
@Component
public class FlowNodeAspect {

    @Autowired
    private TFlowNodeMapper flowNodeMapper;

    @Pointcut("@annotation(com.eden.annotation.FlowNode)")
    public void flowPointCut() {}

    @Around("flowPointCut()")
    public Object recordFlowNode(ProceedingJoinPoint joinPoint) throws Throwable {
        TFlowNode tflowNode = new TFlowNode();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FlowNode flowNode = method.getAnnotation(FlowNode.class);
        tflowNode.setDescription(flowNode.description());
        tflowNode.setOrderNum(flowNode.order());
        setTradeId(joinPoint, tflowNode, signature);
        flowNodeMapper.insert(tflowNode);

        Object proceed;
        try {
            proceed = joinPoint.proceed();
        } catch (Throwable throwable) {
            tflowNode.setStatus(-1);
            flowNodeMapper.updateByPrimaryKey(tflowNode);
            throw throwable;
        }

        tflowNode.setStatus(1);
        flowNodeMapper.updateByPrimaryKey(tflowNode);
        return proceed;
    }

    private void setTradeId(ProceedingJoinPoint joinPoint, TFlowNode tflowNode, MethodSignature signature) {
        String[] parameterNames = signature.getParameterNames();
        int index = ArrayUtils.indexOf(parameterNames, "tradeId");
        if (index > 0) {
            Long tradeId = (Long) joinPoint.getArgs()[index];
            tflowNode.setTradeId(tradeId);
        }
    }
}
