package com.eden.aspect.log.annotation;

import java.lang.annotation.*;

/**
 * @author chenqw
 * @version 1.0
 * @since 2018/11/14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface BusinessLog {

    /**
     * 流程节点描述
     */
    String description() default "";

    /**
     * 流程顺序
     */
    int order();
}
