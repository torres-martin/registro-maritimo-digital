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
String imo = WebUI.callTestCase(findTestCase('Comunes/Crear solicitud'), [('nombreNave') : 'Nave KT35'])
WebUI.closeBrowser()

WebUI.callTestCase(findTestCase('Comunes/Login'), [('correo') : 'funcionario@amp.com'])
WebUI.click(ruta("//tr[contains(., 'IMO" + imo + "')]//a[text()='Revisar']"))
WebUI.delay(2)
WebUI.click(campo('aprobar'))
WebUI.acceptAlert()
WebUI.delay(2)
// Revoca desde "Gestionar certificado"
WebUI.click(ruta("//details[@id='revocacion']/summary"))
WebUI.setText(campo('motivo-revocacion'), 'Revocado por prueba KT-35')
WebUI.click(campo('revocar'))
WebUI.acceptAlert()
WebUI.delay(2)
WebUI.navigateToUrl('http://localhost:8080/bandeja.html')
WebUI.delay(2)
// Abre la bitácora desde la bandeja y busca la solicitud por su IMO
WebUI.click(campo('ir-bitacora'))
WebUI.delay(2)
WebUI.setText(campo('buscar'), 'IMO' + imo)
WebUI.delay(1)
WebUI.verifyElementPresent(ruta("//tbody[@id='bitacora-body']//tr[contains(., 'Revocar') and contains(., 'funcionario@amp.com') and contains(., 'Revocado por prueba KT-35')]"), 5)
WebUI.takeScreenshot()
WebUI.closeBrowser()
