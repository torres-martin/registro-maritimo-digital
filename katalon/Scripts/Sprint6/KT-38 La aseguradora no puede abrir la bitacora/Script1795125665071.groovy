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

WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'aseguradora@amp.com'])
WebUI.navigateToUrl('http://localhost:8080/bitacora.html')
WebUI.delay(2)
WebUI.verifyMatch(WebUI.getUrl(), '.*consulta.*', true)
WebUI.verifyTextNotPresent('Bitácora de auditoría', false)
WebUI.takeScreenshot()
WebUI.closeBrowser()
