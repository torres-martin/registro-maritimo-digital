import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

TestObject campo(String valor) {
    TestObject o = new TestObject(valor)
    o.addProperty('xpath', ConditionType.EQUALS, "//*[@id='" + valor + "']")
    return o
}

TestObject ruta(String xpath) {
    TestObject o = new TestObject(xpath)
    o.addProperty('xpath', ConditionType.EQUALS, xpath)
    return o
}

WebUI.openBrowser('')
WebUI.maximizeWindow()
WebUI.navigateToUrl('http://localhost:8080/verificar.html')
WebUI.setText(campo('codigo'), 'GHNG-ZGA7-JZNV')
WebUI.click(campo('verificar'))
WebUI.delay(2)
WebUI.verifyTextPresent('Nave A', false)
WebUI.verifyTextPresent('Vigente', false)
WebUI.takeScreenshot()
WebUI.closeBrowser()