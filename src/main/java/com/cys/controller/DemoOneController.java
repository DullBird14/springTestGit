package com.cys.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.cys.demoOne.TestClassTwo;

@Controller
public class DemoOneController {
//	@Autowired
//	private TestClass test;
	
	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public String home(){
		System.out.println("come to Here!");
		TestClassTwo two = new TestClassTwo();
		two.test();
//		test.test();
		return "home";
	}
	
	public void test(){
		System.out.println("for test");
	}
}
