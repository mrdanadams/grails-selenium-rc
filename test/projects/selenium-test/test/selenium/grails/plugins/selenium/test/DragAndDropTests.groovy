package grails.plugins.selenium.test

import grails.plugins.selenium.SeleneseTestCategory

@Mixin(SeleneseTestCategory)
class DragAndDropTests extends GroovyTestCase {

	void testDragToTarget() {
		selenium.open "$contextPath/dragdrop.gsp"
		assertEquals "Drop here", selenium.getText("css=#droppable p")
		selenium.dragAndDropToObject("draggable", "droppable")
		waitFor {
			selenium.isElementPresent("css=#droppable.ui-state-highlight")
		}
		assertEquals "Dropped!", selenium.getText("css=#droppable p")
	}

}