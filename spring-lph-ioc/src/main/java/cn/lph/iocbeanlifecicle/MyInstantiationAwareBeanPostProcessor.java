package cn.lph.iocbeanlifecicle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

/**
 * Created by xsls on 2021/12/20.
 */
//@Component
public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if(beanClass== Car.class) {
            Car car = new Car();
            car.setName("通过bean后置处理器返回的");
            return car;
        }
        return null;
    }
}
