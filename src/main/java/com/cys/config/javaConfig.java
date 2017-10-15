package com.cys.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.cys.demoOne.TestClass;

@Configuration
@ComponentScan
/*@ImportResource({"classpath*:beans.xml"})*/
public class javaConfig {
	
    @Bean
    TestClass getTestClass() {
        return new TestClass();
    }
    
    public static void main(String[] args) {
        ApplicationContext context = 
            new AnnotationConfigApplicationContext(javaConfig.class);
        TestClass test = context.getBean(TestClass.class);
        test.test();
    }
}
