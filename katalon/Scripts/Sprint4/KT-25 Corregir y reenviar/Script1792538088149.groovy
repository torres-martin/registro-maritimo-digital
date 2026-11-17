import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.llm.keyword.LlmKeywords as LLM
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

WebUI.openBrowser(null)

WebUI.maximizeWindow()

WebUI.navigateToUrl('http://localhost:8080/login.html')

WebUI.setText(findTestObject('Page_Iniciar sesin Registro Martimo Digital/input_Correo electronico'), 'naviera@amp.com')

WebUI.setEncryptedText(findTestObject('Page_Iniciar sesin Registro Martimo Digital/input_Contrasena'), 'qeJ0sjJnwEs=')

WebUI.click(findTestObject('Page_Iniciar sesin Registro Martimo Digital/button_entrar'))

WebUI.delay(2)

WebUI.click(findTestObject('Page_Mis solicitudes Registro Martimo Digital/a_Corregir y reenviar'))

WebUI.delay(2)

WebUI.verifyTextPresent('Corrigiendo la solicitud', false)

WebUI.verifyTextPresent('Falta la firma del notario', false)

WebUI.takeScreenshot()

WebUI.closeBrowser()

