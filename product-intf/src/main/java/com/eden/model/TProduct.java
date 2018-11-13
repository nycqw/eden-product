package com.eden.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Setter
@Getter
public class TProduct {
    @NotNull(message = "不能为空")
    private Long productId;

    @NotEmpty(message = "不能为空值")
    private String productName;

    private Byte productCategory;

    private Double price;

    private Boolean isSelf;

    private Date startDate;

    private Date endDate;
}