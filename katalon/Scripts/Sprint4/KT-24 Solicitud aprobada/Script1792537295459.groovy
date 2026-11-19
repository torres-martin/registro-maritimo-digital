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
String imo = WebUI.callTestCase(findTestCase('Comunes/Crear solicitud'), [('nombreNave') : 'Nave KT24'])
WebUI.closeBrowser()

WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'funcionario@amp.com'])
WebUI.click(ruta("//tr[contains(., 'IMO" + imo + "')]//a[text()='Revisar']"))
WebUI.delay(2)
WebUI.click(campo('aprobar'))
WebUI.acceptAlert()
WebUI.delay(2)
WebUI.closeBrowser()

// Sprint 7: con el certificado emitido no quedan etapas pendientes
WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'naviera@amp.com'])
WebUI.setText(campo('buscar'), imo)
WebUI.delay(1)
WebUI.click(ruta("//tbody[@id='tabla-body']//tr[contains(., 'Nave KT24')]//button"))
WebUI.delay(2)
WebUI.verifyElementText(campo('det-nave'), 'Nave KT24')
for (String texto : ['Aprobada', 'Certificado emitido', 'Disponible']) {
    WebUI.verifyElementPresent(ruta("//section[@id='det-historial'][contains(., '" + texto + "')]"), 5)
}
WebUI.verifyElementNotPresent(ruta("//section[@id='det-historial'][contains(., 'Pendiente')]"), 2)
WebUI.takeScreenshot()
WebUI.closeBrowser()
