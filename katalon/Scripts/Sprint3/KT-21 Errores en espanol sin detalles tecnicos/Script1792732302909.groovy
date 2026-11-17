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

WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'naviera@amp.com'])

WebUI.navigateToUrl('http://localhost:8080/api/naves/999999')
WebUI.verifyTextPresent('Solicitud no encontrada', false)
WebUI.verifyTextNotPresent('Exception', false)
WebUI.takeScreenshot()

WebUI.navigateToUrl('http://localhost:8080/api/naves/abc')
WebUI.verifyTextPresent('formato', false)
WebUI.verifyTextNotPresent('Exception', false)
WebUI.takeScreenshot()

WebUI.navigateToUrl('http://localhost:8080/api/naves/5/certificado')
WebUI.verifyTextPresent('no tiene certificado', false)
WebUI.verifyTextNotPresent('Exception', false)
WebUI.takeScreenshot()
WebUI.closeBrowser()