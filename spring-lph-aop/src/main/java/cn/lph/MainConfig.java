package cn.lph;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Created by xsls on 2022/01/03.   lph
 */
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true) //(proxyTargetClass = true)  /*<aop:aspectj-autoproxy/>*/
@ComponentScan("cn.lph")
public class MainConfig {

   /* @Bean
    public Calculate calculate() {
        return new TulingCalculate();
    }

    @Bean
    public TulingLogAspect tulingLogAspect() {
        return new TulingLogAspect();
    }


    @Bean
    public Calculate calculate2() {
        return new TulingCalculate();
    }*/
}
