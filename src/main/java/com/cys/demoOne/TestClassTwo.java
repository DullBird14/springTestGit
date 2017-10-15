package com.cys.demoOne;

import org.springframework.beans.factory.annotation.Autowired;

public class TestClassTwo {
	
	private TestClass testClass;
	
	public TestClassTwo(){
	}
	
	@Autowired
	public TestClassTwo(TestClass test){
		testClass = test;
	}
	public void test(){
		System.out.println("my name is TestClassTwo" );
		testClass.test();
	}
}
