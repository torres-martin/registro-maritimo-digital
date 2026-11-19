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

WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'funcionario@amp.com'])
WebUI.click(campo('ir-bitacora'))
WebUI.delay(2)
WebUI.verifyMatch(WebUI.getUrl(), '.*bitacora.*', true)
WebUI.verifyTextPresent('Bitácora de auditoría', false)
WebUI.verifyTextPresent('Fecha y hora', false)
WebUI.verifyTextPresent('Usuario', false)
WebUI.takeScreenshot()
WebUI.click(campo('volver-bandeja'))
WebUI.delay(1)
WebUI.verifyMatch(WebUI.getUrl(), '.*bandeja.*', true)
WebUI.closeBrowser()
