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
WebUI.selectOptionByLabel(campo('filtro-accion'), 'Aprobar', false)
WebUI.delay(1)
// Con el filtro solo quedan filas de Aprobar
WebUI.verifyElementPresent(ruta("//tbody[@id='bitacora-body']//tr[contains(., 'Aprobar')]"), 5)
WebUI.verifyElementNotPresent(ruta("//tbody[@id='bitacora-body']//tr[contains(., 'Enviar solicitud')]"), 2)
WebUI.takeScreenshot()
WebUI.closeBrowser()
