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
String imo = WebUI.callTestCase(findTestCase('Comunes/Crear solicitud'), [('nombreNave') : 'Nave KT23'])
WebUI.closeBrowser()

// Sprint 7: el recorrido tiene 4 etapas (Recibida, Revisión, Resolución, Certificado)
WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'naviera@amp.com'])
WebUI.setText(campo('buscar'), imo)
WebUI.delay(1)
WebUI.click(ruta("//tbody[@id='tabla-body']//tr[contains(., 'Nave KT23')]//button"))
WebUI.delay(2)
WebUI.verifyElementText(campo('det-nave'), 'Nave KT23')
for (String etapa : ['Recibida', 'Revisión', 'Resolución', 'Certificado', 'En curso', 'Pendiente', 'Si se aprueba']) {
    WebUI.verifyElementPresent(ruta("//section[@id='det-historial'][contains(., '" + etapa + "')]"), 5)
}
WebUI.takeScreenshot()
WebUI.closeBrowser()
