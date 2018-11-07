package com.eden;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.eden.mapper")
public class ProductWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductWebApplication.class, args);
	}
}
