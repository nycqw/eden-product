package com.eden.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
public class TProduct implements Serializable {
    private Long id;

    private Long productId;

    private String productName;

    private Byte productCategory;

    private Long stockAmount;

    private Double price;

    private Date startDate;

    private Date endDate;
}