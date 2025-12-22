package com.flowable.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 保险理赔系统主应用类
 * 
 * @author Flowable Demo
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.flowable.demo")
public class InsuranceClaimApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceClaimApplication.class, args);
    }
}
