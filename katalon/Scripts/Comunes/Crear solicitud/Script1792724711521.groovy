import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

TestObject campo(String valor) {
    TestObject o = new TestObject(valor)
    o.addProperty('xpath', ConditionType.EQUALS, "//*[@id='" + valor + "']")
    return o
}

// Si no se indica un IMO, se genera uno con digito de control valido
if (imo == '') {
    Random r = new Random()
    List<Integer> d = [9] + (1..5).collect { r.nextInt(10) }
    int suma = 0
    [7, 6, 5, 4, 3, 2].eachWithIndex { peso, i -> suma += d[i] * peso }
    imo = d.join('') + (suma % 10)
}

WebUI.navigateToUrl('http://localhost:8080/nueva-solicitud.html')
WebUI.delay(1)

// Paso 1: documento y partes
WebUI.selectOptionByLabel(campo('documentoBase'), 'Venta judicial', false)
WebUI.executeJavaScript("var f = document.getElementById('fechaCelebracion'); f.value = '2025-01-15'; f.dispatchEvent(new Event('input', {bubbles: true})); f.dispatchEvent(new Event('change', {bubbles: true}));", null)
WebUI.setText(campo('nombreTransmitente'), 'Transmitente de Prueba S.A.')
WebUI.setText(campo('propietario'), 'Naviera de Prueba S.A.')
WebUI.click(campo('siguiente'))

// Paso 2: nave
WebUI.setText(campo('nombreNave'), nombreNave)
WebUI.setText(campo('imo'), imo)
WebUI.selectOptionByLabel(campo('tipoNave'), 'Granelero', false)
WebUI.click(campo('siguiente'))

// Paso 3: documento y envio
WebUI.setText(campo('solicitante'), 'Solicitante de Prueba')
WebUI.setText(campo('abogado'), 'Abogado de Prueba')
WebUI.executeJavaScript("document.getElementById('pdf').hidden = false;", null)
WebUI.uploadFile(campo('pdf'), archivo)
WebUI.delay(1)

if (enviar) {
    WebUI.click(campo('siguiente'))
    WebUI.delay(2)
}

return imo

