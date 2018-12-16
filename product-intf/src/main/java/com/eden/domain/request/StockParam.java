package com.eden.domain.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author chenqw
 * @version 1.0
 * @since 2018/12/15
 */
@Setter
@Getter
public class StockParam implements Serializable {

    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 购买数量
     */
    private Long number;
    /**
     * 库存数量
     */
    private Long stockAmount;
}
