package ioc.service;

import org.springframework.stereotype.Service;

/**
 * @author lph
 * @description:
 * @date 2021/12/18 14:08
 */
@Service
public class UserServiceImpl {

	public void sayHi(){
		System.out.println("Hello Spring！");
	}

}
