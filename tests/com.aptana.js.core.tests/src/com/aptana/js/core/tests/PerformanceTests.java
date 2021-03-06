package com.aptana.js.core.tests;

import org.junit.runners.Suite.SuiteClasses;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import java.text.MessageFormat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.aptana.js.core.parsing.JSFlexScannerPerformanceTest;
import com.aptana.js.core.parsing.JSParserPerformanceTest;
import com.aptana.js.internal.core.parsing.sdoc.SDocParserPerformanceTest;

@RunWith(Suite.class)
@SuiteClasses({JSFlexScannerPerformanceTest.class, JSParserPerformanceTest.class, SDocParserPerformanceTest.class, })
public class PerformanceTests
{

//	public static Test suite()
//	{
//		TestSuite suite = new TestSuite("JS Core performance tests")
//		{
//			@Override
//			public void runTest(Test test, TestResult result)
//			{
//				String msg = MessageFormat.format("Running test: {0}", test.toString());
//				System.err.println(msg);
//				super.runTest(test, result);
//			}
//		};
//
//		// $JUnit-BEGIN$
//		suite.addTestSuite(JSFlexScannerPerformanceTest.class);
//		suite.addTestSuite(JSParserPerformanceTest.class);
//		suite.addTestSuite(SDocParserPerformanceTest.class);
//		// $JUnit-END$
//		return suite;
//	}
//
}
