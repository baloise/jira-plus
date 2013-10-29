package com.baloise.confluence.rest;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class JiraPlusConfigModel {

	@XmlElement
	private String name;
	@XmlElement
	private int time;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}
}