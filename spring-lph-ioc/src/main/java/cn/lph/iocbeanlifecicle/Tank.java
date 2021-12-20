package cn.lph.iocbeanlifecicle;

import org.springframework.stereotype.Component;

/**
 * Created by xsls on 2021/12/20.
 */
@Component
public class Tank  {


	private Integer index;

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}


	public Tank() {
		System.out.println("tank加载");
	}



}
