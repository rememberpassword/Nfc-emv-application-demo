package com.rmp.emvengine

import com.rmp.emvengine.common.CommandHelper
import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.Capk
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.toTlvObjects
import com.rmp.emvengine.process.oda.OdaProcessHelper
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        var len = 0
        len = len shl 8
        len = len or 0xb7
            val x = "9081B08828A01BD14335BBD0607E7700E9F13D7B367CB6AD7440C0C6521B391602B946AD1E527D779D496EF696FC2ABD6EB91C48899AA7CAF73A80A7BE69403E49FB93E530DBC536890D191EEAD9A513003304CAD3970488EF4E98EB3EA9867E1DDA552D38479D3F1379C22C1A61C625E5F2FB5845A7E590494BC0277368A71C1EC97EBE2ACDA6051B22B39BA0FC8F23A8BC9AFE964ECE21AFF0CCFE2CEED7D44ABEF88798DEB97F73920AD65F91FCD8A9E02E5F340101".hexToByteArray()
        val y = x.toTlvObjects()
        print( y)

    }

    val cardData = mutableMapOf<Long,TlvObject>()
    private fun parseGPO(data: ByteArray): Boolean {
        data.toTlvObjects()?.firstOrNull {
            it.tag == 0x77L
        }?.also {
            it.value.toTlvObjects()?.forEach {
//                cardData[it.tag] = it
                if (cardData[it.tag] == null) {
                    cardData[it.tag] = it
                } else {
//                    lastError = "4"
                    println(it.tag.toString(16))
                    return false
                }

            }
        } ?: return false
        return true
    }




    @Test
    fun testDDA(){
      val masterCapkF1 = Capk(
          index = 0xF1,
          rid = "A000000004".hexToByteArray(),
          modulus = "996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9F".hexToByteArray(),
          exponent = "03".hexToByteArray(),
          sha = null
      )
        val issuerCer = "8828A01BD14335BBD0607E7700E9F13D7B367CB6AD7440C0C6521B391602B946AD1E527D779D496EF696FC2ABD6EB91C48899AA7CAF73A80A7BE69403E49FB93E530DBC536890D191EEAD9A513003304CAD3970488EF4E98EB3EA9867E1DDA552D38479D3F1379C22C1A61C625E5F2FB5845A7E590494BC0277368A71C1EC97EBE2ACDA6051B22B39BA0FC8F23A8BC9AFE964ECE21AFF0CCFE2CEED7D44ABEF88798DEB97F73920AD65F91FCD8A9E02E".hexToByteArray()
        val issuerExp = "03".hexToByteArray()
        val issuerRemainder = "95B60A462970C7F18237F59FA5BB675C4185922EC4B13397FD623346AACE0416AAAD33DF".hexToByteArray()

        val issuerPk = OdaProcessHelper.decipherIssuerPublicKey(
            capk = masterCapkF1,
            issuerCertificate = issuerCer,
            issuerExponent = issuerExp,
            issuerPublicKeyRemainder = issuerRemainder
            )
        println("issuerMod:${issuerPk?.modulus?.toHexString()}")
        println("issuerExp:${issuerPk?.exponent?.toHexString()}")
        val iccCert = "202B8FB78ADE6393DDAAB41682588CCF67459B2745F0868CD0E14AADEFA8EE535DA681A399850AD200A1002D795659CA739C4387B298CA61D07A4CB70DB2B6D64272180A207D2B913494188EDD3E6E333E089702F56552236FCABE16FA445C0A3FB75A48E7B1200D686F28BA35074FB90CE13535F3102969B9D563E45906EC936AE6DFF8F5B562E315DD97A5A68410216A930B332A495E5C5C2AF919E8987546697A7FA5DFFD907BC68C63FA5E04CE9A".hexToByteArray()
        val iccExp = "03".hexToByteArray()
        val iccRemainder = "8838AE2A3FF6E8006F310E4C1ADD14497F20DC0DD46AD1F1D8B5D91C80358F6B1BB83E24BD1B0C4E3331".hexToByteArray()
        val offlineAuthenticationRecords = "5A0841766622200100185F2403241231".hexToByteArray()
        val dataOfSDATagList: ByteArray? = null //only contain 82

        val iccPk = OdaProcessHelper.decipherICCPublicKey(
            issuerPublicKey = issuerPk!!,
            iccPKCertificate= iccCert,
            iccExponent = iccExp,
            iccPublicKeyRemainder= iccRemainder,
            offlineAuthenticationRecords = offlineAuthenticationRecords,
            aip =  dataOfSDATagList
        )

        val sdad = "1A2AD9501EAC03C21FB3EF22724599C5C84A37E00249B2B179513E4E8BD0A3D3E51E149AEA08B26F6E4CC99F70C8A5F09A8F7CF6CC6E1C75BD84C8945CE871F46F634095D4513E782A8327BC1775ED8CC2469C056B2438524C55D3BA9594A6E268998359E130982B080E9B473441E1C7B5C3FDD7D84095BCE9B7DB8087C3132A2F7BCDB3B7DC1781A05E9AFD543763180D2D1D2E9E99C3619622268BAF0ACD2C3B81B2DA1AAFBD773E86990B2987B8FC".hexToByteArray()
        val terminalDynamicData = "2C975EAC000000001800097801253996170000".hexToByteArray()

        val result = OdaProcessHelper.verifyDDA(
            iccPublicKey = iccPk!!,
            sdad = sdad,
            terminalDynamicData = terminalDynamicData
        )

        println("result: $result")



    }

    @Test
    fun testSDA(){
        val capkF1 = Capk(
            index = 0xF1,
            rid = "A000000004".hexToByteArray(),
            modulus = "AF0754EAED977043AB6F41D6312AB1E22A6809175BEB28E70D5F99B2DF18CAE73519341BBBD327D0B8BE9D4D0E15F07D36EA3E3A05C892F5B19A3E9D3413B0D97E7AD10A5F5DE8E38860C0AD004B1E06F4040C295ACB457A788551B6127C0B29".hexToByteArray(),
            exponent = "03".hexToByteArray(),
            sha = null
        )
        val issuerCer = "191AB5AC03365D5E9515C398CCC5C744A728A4FCFDE194D0B88B0FA1673AEBDD8AAADF0EDBBC12414E7107A9F2B02DFB3985167C0EE9CDF3CB78749BF6D0AAE60E4C979F7E2AE635A77451B0E2F2EB136AB02076CBE1E70CC4EE5529434A9EC6".hexToByteArray()
        val issuerExp = "03".hexToByteArray()
        val issuerRemainder = "CFB8D4885D960967179F982D42CE54ECC2054683".hexToByteArray()

        val issuerPk = OdaProcessHelper.decipherIssuerPublicKey(
            capk = capkF1,
            issuerCertificate = issuerCer,
            issuerExponent = issuerExp,
            issuerPublicKeyRemainder = issuerRemainder
        )
        println("issuerMod:${issuerPk?.modulus?.toHexString()}")
        println("issuerExp:${issuerPk?.exponent?.toHexString()}")

        val sdad = "110BB9DF2D21981906B29A301411F9FA60CF494DBABABF54B1797C9C4B5D99B5E67AB73049E771FC5FDC23E58350B781005324D31DC87AD0FBF636733808056D66074632711E7CBF14073796E1B60D4D".hexToByteArray()
        val odaRecord = "5A0847617390010100105F340101".hexToByteArray()

        val result = OdaProcessHelper.verifySDA(
            issuerPublicKey = issuerPk!!,
            sdad = sdad,
            offlineAuthenticationRecords = odaRecord
        )

        println("result: $result")

    }

    @Test
    fun cccc(){

        val afl = "1801010110010300".hexToByteArray()
        val cmds = CommandHelper.buildReadRecordCmdByAFL(afl)
        cmds.forEach {
            println("cmd:${it.first.toHexString()},oda:${it.second}")
        }
        val x =1

    }



}
