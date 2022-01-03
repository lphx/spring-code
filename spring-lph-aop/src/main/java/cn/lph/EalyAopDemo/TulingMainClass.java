package cn.lph.EalyAopDemo;

import cn.lph.Calculate;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


/**
 * Created by xsls on 2022/01/03.   lph
 */
public class TulingMainClass {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(EalyAopMainConfig.class);
        Calculate tulingCalculate = ctx.getBean("tulingCalculate",Calculate.class);
        tulingCalculate.div(1,1);

        /*AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(EalyAopMainConfig.class);
        Calculate calculateProxy = ctx.getBean("calculateProxy",Calculate.class);
        System.out.println(calculateProxy.getClass());
        calculateProxy.div(1,1);*/

    }
}
