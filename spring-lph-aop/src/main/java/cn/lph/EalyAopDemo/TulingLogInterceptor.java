package cn.lph.EalyAopDemo;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/***
 * Created by xsls on 2022/01/03.   lph
 */
public class TulingLogInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println(getClass()+"调用方法前");
        Object ret=invocation.proceed();
        System.out.println(getClass()+"调用方法后");
        return ret;
    }

}
