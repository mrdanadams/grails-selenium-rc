/*
 * Copyright 2010 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.selenium

import com.thoughtworks.selenium.CommandProcessor
import com.thoughtworks.selenium.Selenium
import com.thoughtworks.selenium.Wait.WaitTimedOutException
import grails.test.GrailsUnitTestCase
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.gmock.WithGMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.hasItem

@WithGMock
class SeleniumWrapperTests extends GrailsUnitTestCase {

	private static final DEFAULT_TIMEOUT = "10000"

	CommandProcessor mockCommandProcessor
	Selenium mockSelenium
	SeleniumWrapper seleniumWrapper

	@Before
	void setUp() {
		super.setUp()

		registerMetaClass(SeleniumWrapper)

		def config = new ConfigSlurper().parse("""
			selenium {
				defaultTimeout = "$DEFAULT_TIMEOUT"
			}
		""")

		mockCommandProcessor = mock(CommandProcessor)
		mockSelenium = mock(Selenium)
		seleniumWrapper = new SeleniumWrapper(mockSelenium, mockCommandProcessor, config)
	}

	@After
	void tearDown() {
		super.tearDown()
		ConfigurationHolder.config = null
		ApplicationHolder.application = null
	}


	@Test
	void getAndSetTimeoutOperateCorrectly() {
		mockSelenium.setTimeout("5000")

		play {
			seleniumWrapper.setTimeout("5000")
		}

		assertThat "timeout", seleniumWrapper.getTimeout(), equalTo("5000")
	}

	@Test
	void isAliveReportsCorrectStateAfterStartAndStopCalls() {
		ordered {
			mockSelenium.start()
			mockSelenium.stop()
		}

		play {
			assertThat "selenium alive", seleniumWrapper.alive, equalTo(false)
			seleniumWrapper.start()
			assertThat "selenium alive", seleniumWrapper.alive, equalTo(true)
			seleniumWrapper.stop()
			assertThat "selenium alive", seleniumWrapper.alive, equalTo(false)
		}
	}

	@Test
	void defaultTimeoutIsDeterminedByConfig() {
		assertThat "default timeout", seleniumWrapper.defaultTimeout, equalTo(DEFAULT_TIMEOUT)
	}

	@Test
	void waitForPageToLoadWithNoArgsDelegatesToSeleniumMethodUsingTimeout() {
		mockSelenium.setTimeout("5000")
		mockSelenium.waitForPageToLoad("5000")

		play {
			seleniumWrapper.timeout = "5000"
			seleniumWrapper.waitForPageToLoad()
		}
	}

	@Test
	void waitForPageToLoadWithNoArgsDelegatesToSeleniumMethodUsingDefaultTimeout() {
		mockSelenium.waitForPageToLoad(DEFAULT_TIMEOUT)

		play {
			seleniumWrapper.waitForPageToLoad()
		}
	}

	@Test
	void getContextPathReturnsAppNameIfContextNotSetInConfig() {
		def appName = "foo"
		ConfigurationHolder.config = new ConfigObject()
		ApplicationHolder.application = mock(GrailsApplication) {
			metadata.returns("app.name": appName).stub()
		}

		play {
			assertThat "context path", seleniumWrapper.contextPath, equalTo("/$appName" as String)
		}
	}

	@Test
	void getContextPathReturnsAppContextFromConfigIfSet() {
		def context = "/foo"
		ConfigurationHolder.config = new ConfigSlurper().parse("grails.app.context = '$context'")

		play {
			assertThat "context path", seleniumWrapper.contextPath, equalTo(context)
		}
	}

	@Test
	void canAppendAndWaitToSeleniumMethods() {
		ordered {
			mockSelenium.click("whatever")
			mockSelenium.waitForPageToLoad(DEFAULT_TIMEOUT)
		}

		play {
			seleniumWrapper.clickAndWait("whatever")
		}
	}

	@Test
	void canAppendAndWaitToSeleniumMethodsWithNoArguments() {
		ordered {
			mockSelenium.refresh()
			mockSelenium.waitForPageToLoad(DEFAULT_TIMEOUT)
		}

		play {
			seleniumWrapper.refreshAndWait()
		}
	}

	@Test
	void canWaitForSeleniumMethodThatReturnsBoolean() {
		mockSelenium.isTextPresent("whatever").returns(false).times(2)
		mockSelenium.isTextPresent("whatever").returns(true)

		play {
			assertTrue seleniumWrapper.waitForTextPresent("whatever")
		}
	}

	@Test
	void canWaitForSeleniumMethodThatReturnsBooleanNegated() {
		mockSelenium.isTextPresent("whatever").returns(true).times(2)
		mockSelenium.isTextPresent("whatever").returns(false)

		play {
			assertTrue seleniumWrapper.waitForNotTextPresent("whatever")
		}
	}

	@Test
	void canWaitForSeleniumMethodThatReturnsString() {
		mockSelenium.getText("whatever").returns("incorrect").times(2)
		mockSelenium.getText("whatever").returns("correct")

		play {
			assertTrue seleniumWrapper.waitForText("whatever", "correct")
		}
	}

	@Test
	void canWaitForSeleniumMethodThatReturnsStringNegated() {
		mockSelenium.getText("whatever").returns("incorrect").times(2)
		mockSelenium.getText("whatever").returns("correct")

		play {
			assertTrue seleniumWrapper.waitForNotText("whatever", "incorrect")
		}
	}

	@Test
	void canWaitForNoArgsSeleniumMethodThatReturnsString() {
		mockSelenium.getTitle().returns("incorrect").times(2)
		mockSelenium.getTitle().returns("correct")

		play {
			assertTrue seleniumWrapper.waitForTitle("correct")
		}
	}

	@Test
	void canWaitForNoArgsSeleniumMethodThatReturnsStringNegated() {
		mockSelenium.getTitle().returns("incorrect").times(2)
		mockSelenium.getTitle().returns("correct")

		play {
			assertTrue seleniumWrapper.waitForNotTitle("incorrect")
		}
	}

	@Test
	void canWaitForSeleniumMethodThatRetunsStringToMatchHamcrestMatcher() {
		mockSelenium.getText("whatever").returns("incorrect").times(2)
		mockSelenium.getText("whatever").returns("correct")

		play {
			assertTrue seleniumWrapper.waitForText("whatever", equalTo("correct"))
		}
	}

	@Test
	void canWaitForSeleniumMethodThatRetunsStringToMatchRegexPattern() {
		mockSelenium.getText("whatever").returns("incorrect").times(2)
		mockSelenium.getText("whatever").returns("correct")

		play {
			assertTrue seleniumWrapper.waitForText("whatever", ~/c\w+/)
		}
	}

	@Test
	void canWaitForSeleniumMethodThatRetunsStringToMatchRegexPatternNegated() {
		mockSelenium.getText("whatever").returns("incorrect").times(2)
		mockSelenium.getText("whatever").returns("correct")

		play {
			assertTrue seleniumWrapper.waitForNotText("whatever", ~/in\w+/)
		}
	}

	@Test(expected = MissingMethodException)
	void cannotUseWaitForNotWithHamcrestMatcher() {
		seleniumWrapper.waitForNotText("whatever", equalTo("correct"))
	}

	@Test(expected = WaitTimedOutException)
	void waitForThrowsExceptionOnTimeout() {
		mockSelenium.setTimeout("1000")
		mockSelenium.isTextPresent("whatever").returns(false).stub()

		play {
			seleniumWrapper.timeout = "1000" // otherwise this test will take 30 seconds
			seleniumWrapper.waitForTextPresent("whatever")
		}
	}

	@Test
	void waitForGeneratesCorrectMessageOnTimeout() {
		mockSelenium.setTimeout("1000")
		mockSelenium.getText("whatever").returns("incorrect").stub()

		play {
			seleniumWrapper.timeout = "1000" // otherwise this test will take 30 seconds
			try {
				seleniumWrapper.waitForText("whatever", "correct")
				fail "Should have throw WaitTimedOutException"
			} catch (WaitTimedOutException e) {
				assertThat e.message, equalTo("Timed out waiting for getText(whatever) to be \"correct\"")
			}
		}
	}

	@Test(expected = MissingMethodException)
	void waitForFailsForInvalidSeleniumMethod() {
		seleniumWrapper.waitForBlahBlahBlah("foo")
	}

	@Test
	void dynamicMethodsAreAddedToMetaClassAfterBeingInvoked() {
		mockSelenium.click("whatever").stub()
		mockSelenium.waitForPageToLoad(DEFAULT_TIMEOUT).stub()

		play {
			assertThat "metaClass methods matching 'clickAndWait'", seleniumWrapper.metaClass.respondsTo(seleniumWrapper, "clickAndWait"), equalTo([])
			seleniumWrapper.clickAndWait("whatever")
			assertThat "metaClass methods matching 'clickAndWait'", seleniumWrapper.metaClass.respondsTo(seleniumWrapper, "clickAndWait"), hasItem(not(nullValue()))
		}
	}

	@Test
	void delegatesMissingMethodsToUserExtensionScripts() {
		mockCommandProcessor.doCommand(equalTo("someUserExtension"), equalTo(["foo", "bar"] as String[])).returns("OK,result")

		play {
			assertThat "result from user extension script", seleniumWrapper.someUserExtension("foo", "bar"), equalTo("result")
		}
	}

	@Test(expected = MissingMethodException)
	void methodsWithNonStringArgsAreNotTreatedAsUserExtensionCalls() {
		play {
			seleniumWrapper.whatever("foo", 1)
		}
	}

	@Test
	void canUseWaitForWithClosure() {
		mockSelenium.getText("whatever").returns("incorrect").times(2)
		mockSelenium.getText("whatever").returns("correct")

		play {
			seleniumWrapper.waitFor("whatever to be correct") {
				seleniumWrapper.getText("whatever") == "correct"
			}
		}
	}

	@Test(expected = WaitTimedOutException)
	void waitForWithClosureFailsOnTimeout() {
		mockSelenium.setTimeout("1000")
		mockSelenium.getText("whatever").returns("incorrect").stub()

		play {
			seleniumWrapper.timeout = "1000" // otherwise this test will take 30 seconds
			seleniumWrapper.waitFor("whatever to be correct") {
				seleniumWrapper.getText("whatever") == "correct"
			}
		}
	}
}
