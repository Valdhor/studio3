/**
 * Aptana Studio
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.css.core.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.aptana.css.core.parsing.CSSParserPerformanceTest;
import com.aptana.css.core.parsing.CSSScannerPerformanceTest;

@RunWith(Suite.class)
@SuiteClasses({ CSSScannerPerformanceTest.class, CSSParserPerformanceTest.class, })
public class PerformanceTests
{
}
