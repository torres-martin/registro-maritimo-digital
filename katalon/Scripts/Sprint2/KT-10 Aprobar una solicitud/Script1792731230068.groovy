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
String imo = WebUI.callTestCase(findTestCase('Comunes/Crear solicitud'), [('nombreNave') : 'Nave KT10'])
WebUI.closeBrowser()

WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'funcionario@amp.com'])
WebUI.click(ruta("//tr[contains(., 'IMO" + imo + "')]//a[text()='Revisar']"))
WebUI.delay(2)
WebUI.click(campo('aprobar'))
WebUI.acceptAlert()
WebUI.delay(2)
WebUI.verifyTextPresent('Solicitud aprobada', false)
WebUI.verifyElementNotVisible(campo('aprobar'))
WebUI.takeScreenshot()
WebUI.closeBrowser()