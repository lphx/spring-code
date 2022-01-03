package cn.lph;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by xsls on 2022/01/03.   lph
 */
public class TulingMainClass {

    public static void main(String[] args) {

    	AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

        Calculate calculate = (Calculate) ctx.getBean("tulingCalculate");
         int retVal = calculate.add(2,4);

        /*ProgramCalculate calculate = (ProgramCalculate) ctx.getBean("tulingCalculate");
        System.out.println(calculate.toBinary(100));*/
    }
}
