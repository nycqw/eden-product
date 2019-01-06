package com.eden.aspect.onoff;

import com.eden.aspect.onoff.annotation.Switch;
import com.eden.exception.SwitchCloseException;
import com.eden.mapper.TSwitchMapper;
import com.eden.model.TSwitch;
import com.eden.util.AopUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenqw
 * @version 1.0
 * @since 2019/1/5
 */
@Aspect
@Component
@Slf4j
public class SwitchAspect {

    // 锁根路径
    private final String SWITCH_ROOT = "/SWITCH_ROOT/";
    private final String DEFAULT_SWITCH_STATUS = "ON";
    private Map switchMap = new ConcurrentHashMap<String, String>();

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private TSwitchMapper switchMapper;

    @Pointcut(value = "@annotation(com.eden.aspect.onoff.annotation.Switch)")
    public void switchPoint() {
    }

    @Around(value = "switchPoint()")
    public Object handler(ProceedingJoinPoint joinPoint) throws Throwable {
        String switchPath = getSwitchPath(joinPoint);
        monitorSwitchStatus(switchPath);
        String switchStatus = (String) switchMap.get(switchPath);
        if (!DEFAULT_SWITCH_STATUS.equals(switchStatus)) {
            throw new SwitchCloseException(switchPath);
        }
        return joinPoint.proceed();
    }

    private void monitorSwitchStatus(String switchPath) {
        try {
            if (curatorFramework.checkExists().forPath(switchPath) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(switchPath, DEFAULT_SWITCH_STATUS.getBytes());

                // 初始化开关状态
                initSwitchStatus(switchPath);

                // 监听开关状态
                switchStatusListener(switchPath);
            }
        } catch (Exception e) {
            log.error("监听开关状态异常!", e);
        }
    }

    private void initSwitchStatus(String switchPath) {
        TSwitch entity = switchMapper.selectByName(switchPath);
        if (entity == null) {
            entity = new TSwitch();
            entity.setName(switchPath);
            entity.setStatus(DEFAULT_SWITCH_STATUS);
            entity.setCreateTime(new Date());
            switchMapper.insert(entity);
            switchMap.put(switchPath, DEFAULT_SWITCH_STATUS);
        } else {
            switchMap.put(switchPath, entity.getStatus());
        }
    }

    private void switchStatusListener(String switchPath) throws Exception {
        NodeCache nodeCache = new NodeCache(curatorFramework, switchPath, false);
        nodeCache.start(true);
        nodeCache.getListenable().addListener(() -> {
            String currentSwitchStatus = new String(nodeCache.getCurrentData().getData());
            TSwitch tSwitch = switchMapper.selectByName(switchPath);
            tSwitch.setStatus(currentSwitchStatus);
            tSwitch.setUpdateTime(new Date());
            switchMapper.updateByPrimaryKey(tSwitch);

            switchMap.put(switchPath, currentSwitchStatus);
            if (DEFAULT_SWITCH_STATUS.equals(currentSwitchStatus)) {
                log.info("开关【{}】开启", switchPath);
            } else {
                log.info("开关【{}】关闭", switchPath);
            }
        });
    }

    private String getSwitchPath(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        Method method = AopUtil.getTargetMethod(joinPoint);
        Switch annotation = method.getAnnotation(Switch.class);
        return SWITCH_ROOT + annotation.switchType();
    }
}
