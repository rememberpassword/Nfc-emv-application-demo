package com.rmp.emvengine.process.oda

import com.rmp.emvengine.common.LogUtils
import com.rmp.emvengine.common.RSAHelper
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.Capk
import com.rmp.emvengine.data.RSAPublicKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.NoSuchAlgorithmException
import java.util.*

class OdaProcess {


    /**
     * Decipher Issuer Public Key
     * @param capk The CA public key
     * @param issuerCertificate the issuer certificate (Tag 90)
     * @param issuerExponent The Issuer Exponent (Tag 9F32)
     * @param issuerPublicKeyRemainder The issuer pk remainder (Tag 92)
     */
    fun decipherIssuerPublicKey(
        capk: Capk,
        issuerCertificate: ByteArray,
        issuerExponent: ByteArray,
        issuerPublicKeyRemainder: ByteArray? = null
    ): RSAPublicKey? {

        //Decipher data using RSA
        val dataDecipher = RSAHelper.decipher(issuerCertificate,capk.modulus,capk.exponent)

        val bis = ByteArrayInputStream(dataDecipher)

        if (bis.read() != 0x6a) { //Header
            
            LogUtils.e("Header != 0x6a")
            return null
        }
      
        val certFormat = bis.read()

        if (certFormat.toInt() != 0x02) {
            LogUtils.e("Invalid certificate format")
            return null
        }

        val issuerIdentifierPaddedBytes = ByteArray(4)

        bis.read(issuerIdentifierPaddedBytes, 0, issuerIdentifierPaddedBytes.size)

        //Remove padding (if any) from issuerIdentifier

        //Remove padding (if any) from issuerIdentifier
        var iiStr = issuerIdentifierPaddedBytes.toHexString()
        val padStartIndex = iiStr.uppercase(Locale.ROOT).indexOf('F')
        if (padStartIndex != -1) {
            iiStr = iiStr.substring(0, padStartIndex)
        }
        val issuerIdentifier = iiStr

        val certExpirationDate = ByteArray(2)
        bis.read(certExpirationDate, 0, certExpirationDate.size)

        val certSerialNumber = ByteArray(3)
        bis.read(certSerialNumber, 0, certSerialNumber.size)

        val hashAlgorithmIndicator = bis.read() and 0xFF

        val issuerPublicKeyAlgorithmIndicator = bis.read() and 0xFF

        val issuerPublicKeyModLengthTotal = bis.read() and 0xFF

        val issuerPublicKeyExpLengthTotal = bis.read() and 0xFF

        var modBytesLength = bis.available() - 21

        if (issuerPublicKeyModLengthTotal < modBytesLength) {
            //The mod bytes block in this cert contains padding.
            //we don't want padding in our key
            modBytesLength = issuerPublicKeyModLengthTotal
        }

        //issuer public key modulus
        val modtmp = ByteArray(modBytesLength)

        bis.read(modtmp, 0, modtmp.size)
        val modFull = modtmp + (issuerPublicKeyRemainder ?: byteArrayOf())

        //Now read padding bytes (0xbb), if available
        //The padding bytes are not used

        //Now read padding bytes (0xbb), if available
        //The padding bytes are not used
        val padding = ByteArray(bis.available() - 21)
        bis.read(padding, 0, padding.size)

        val hash = ByteArray(20)
        bis.read(hash, 0, hash.size)


        val hashStream = ByteArrayOutputStream()

        hashStream.write(certFormat.toInt())
        hashStream.write(issuerIdentifierPaddedBytes, 0, issuerIdentifierPaddedBytes.size)
        hashStream.write(certExpirationDate, 0, certExpirationDate.size)
        hashStream.write(certSerialNumber, 0, certSerialNumber.size)
        hashStream.write(hashAlgorithmIndicator)
        hashStream.write(issuerPublicKeyAlgorithmIndicator)
        hashStream.write(issuerPublicKeyModLengthTotal)
        hashStream.write(issuerPublicKeyExpLengthTotal)
        hashStream.write(modFull, 0, modFull.size)
        hashStream.write(issuerExponent, 0, issuerExponent.size)

        var sha1Result: ByteArray? = null
        sha1Result = try {
            RSAHelper.calculateSHA1(hashStream.toByteArray())
        } catch (ex: NoSuchAlgorithmException) {
            LogUtils.e("SHA-1 hash algorithm not available")
            return null
            return null
        }

        if (!Arrays.equals(sha1Result, hash)) {

            LogUtils.e("Hash is not valid")
            return null
        }

        val trailer = bis.read()
        if (trailer != 0xbc) { //Trailer
            LogUtils.e("Trailer != 0xbc")
            return null
        }

        if (bis.available() > 0) {
            LogUtils.e("Error parsing certificate. Bytes left=" + bis.available())
            return null
        }

        return RSAPublicKey(
            modtmp + (issuerPublicKeyRemainder ?: byteArrayOf()),
            issuerExponent
        )
    }

    /**
     * Decipher ICC Public Key
     * @param issuerPublicKey The issuerPublicKey from decipherIssuerPublicKey()
     * @param iccPKCertificate The icc Public Key Certificate (Tag 9F46)
     * @param iccExponent The icc Public Key Exponent (Tag 9F47)
     * @param iccPublicKeyRemainder The icc Public Key Remainder (Tag 9F48)
     * @param offlineAuthenticationRecords The offline authentication record from read record step (Format :TLV1,TLV2,..TLVn)
     * @param aip: The value of Application Interchange Profile
     */
    fun decipherICCPublicKey(
        iccPKCertificate: ByteArray,
        issuerPublicKey: RSAPublicKey,
        iccExponent: ByteArray,
        iccPublicKeyRemainder: ByteArray? = null,
        offlineAuthenticationRecords: ByteArray,
        aip: ByteArray? = null
    ): RSAPublicKey? {

        val recoveredBytes: ByteArray = RSAHelper.decipher(
            dataEncipher = iccPKCertificate,
            exponent = issuerPublicKey.exponent,
            modulus = issuerPublicKey.modulus
        )

        val bis = ByteArrayInputStream(recoveredBytes)

        if (bis.read() != 0x6a) { //Header
            LogUtils.e("Header != 0x6a")
            return null
        }

        val certFormat = bis.read().toByte()

        if (certFormat.toInt() != 0x04) { //Always 0x04
            LogUtils.e("Invalid certificate format")
            return null
        }
        val pan = ByteArray(10)
        bis.read(pan, 0, pan.size)

        val certExpirationDate = ByteArray(2)
        val certSerialNumber = ByteArray(3)
        bis.read(certExpirationDate, 0, certExpirationDate.size)

        bis.read(certSerialNumber, 0, certSerialNumber.size)

        val hashAlgorithmIndicator = bis.read() and 0xFF

        val iccPublicKeyAlgorithmIndicator = bis.read() and 0xFF

        val iccPublicKeyModLengthTotal = bis.read() and 0xFF

        val iccPublicKeyExpLengthTotal = bis.read() and 0xFF

        var modBytesLength = bis.available() - 21

        if (iccPublicKeyModLengthTotal < modBytesLength) {
            //The mod bytes block in the cert contains padding
            //we don't want padding in our key
            modBytesLength = iccPublicKeyModLengthTotal
        }

        val modtmp = ByteArray(modBytesLength)

        bis.read(modtmp, 0, modtmp.size)

        val modFull = modtmp + (iccPublicKeyRemainder ?: byteArrayOf())

        //Now read padding bytes (0xbb), if available
        //The padding bytes are not used

        //Now read padding bytes (0xbb), if available
        //The padding bytes are not used
        val padding = ByteArray(bis.available() - 21)
        bis.read(padding, 0, padding.size)

        val hash = ByteArray(20)
        bis.read(hash, 0, hash.size)

        val hashStream = ByteArrayOutputStream()

        //Header not included in hash

        //Header not included in hash
        hashStream.write(certFormat.toInt())
        hashStream.write(pan, 0, pan.size)
        hashStream.write(certExpirationDate, 0, certExpirationDate.size)
        hashStream.write(certSerialNumber, 0, certSerialNumber.size)
        hashStream.write(hashAlgorithmIndicator)
        hashStream.write(iccPublicKeyAlgorithmIndicator)
        hashStream.write(iccPublicKeyModLengthTotal)
        hashStream.write(iccPublicKeyExpLengthTotal)
        val numPadBytes: Int = issuerPublicKey.modulus.size - 42 - modFull.size
//        LogUtils.d("issuerMod: " + modFull.size.toString() + " iccMod: " + ipkModulus.size.toString() + " padBytes: " + numPadBytes)
        if (numPadBytes > 0) {
            //If NIC <= NI – 42, consists of the full
            //ICC Public Key padded to the right
            //with NI – 42 – NIC bytes of value
            //'BB'
            hashStream.write(modFull, 0, modFull.size)
            for (i in 0 until numPadBytes) {
                hashStream.write(0xBB)
            }
        } else {
            //If NIC > NI – 42, consists of the NI –
            //42 most significant bytes of the
            //ICC Public Key
            //and the NIC – NI + 42 least significant bytes of the ICC Public Key
            hashStream.write(modFull, 0, modFull.size)
        }

        hashStream.write(iccExponent, 0, iccExponent.size)

//        val offlineAuthenticationRecords: ByteArray = "5A0841766622200100185F2403241231".hexToByteArray()
        hashStream.write(offlineAuthenticationRecords, 0, offlineAuthenticationRecords.size)
        //Trailer not included in hash
        aip?.let {
            hashStream.write(it,0,it.size)
        }


        //Trailer not included in hash

        var sha1Result: ByteArray? = null
        sha1Result = try {
            RSAHelper.calculateSHA1(hashStream.toByteArray())
        } catch (ex: NoSuchAlgorithmException) {
            LogUtils.e("SHA-1 hash algorithm not available")
            return null
        }

        if (!Arrays.equals(sha1Result, hash)) {
            LogUtils.e("Hash is not valid")
            return null
        }


        val trailer = bis.read()

        if (trailer != 0xbc) { //Trailer
            LogUtils.e("Trailer != 0xbc")
            return null
        }

        if (bis.available() > 0) {
            LogUtils.e("Error parsing certificate. Bytes left=" + bis.available())
            return null
        }

        return RSAPublicKey(
            modulus = modFull,
            exponent = iccExponent
        )
    }

    /**
     * Verify DDA.
     * @param iccPublicKey The icc Public Key form decipherICCPublicKey()
     * @param sdad The Signed Dynamic Application Data from response of Cmd Internal Authenticate(contact) or from tag 9F4B of record in Visa, Upi(cless).
     * @param terminalDynamicData The data pass thought Cmd Internal Authenticate (contact) or value of 9F37,9F02,5F2A,9F69 in  Visa, Upi(cless).
     */
    fun verifyDDA(
        iccPublicKey: RSAPublicKey,
        sdad: ByteArray,
        terminalDynamicData: ByteArray
    ): Boolean{
        val decipheredData: ByteArray = RSAHelper.decipher(
            dataEncipher = sdad,
            exponent = iccPublicKey.exponent,
            modulus = iccPublicKey.modulus
        )
        val stream = ByteArrayInputStream(decipheredData)

        val header = stream.read().toByte()

        if (header != 0x6a.toByte()) {
            LogUtils.e("Header != 0x6a")
            return false
        }

        val signedDataFormat = stream.read().toByte()

        if (signedDataFormat != 0x05.toByte()) {
            LogUtils.e("Signed Data Format != 0x05")
            return false
        }

        val hashAlgorithmIndicator = stream.read().toByte() //We currently only support SHA-1

        val iccDynamicDataLength = stream.read()

        val iccDynamicNumber = ByteArray(iccDynamicDataLength)

        stream.read(iccDynamicNumber, 0, iccDynamicDataLength)

        //Now read padding bytes (0xbb), if available
        //The padding bytes are used in hash validation

        //Now read padding bytes (0xbb), if available
        //The padding bytes are used in hash validation
        val padding = ByteArray(stream.available() - 21)
        stream.read(padding, 0, padding.size)

        val hashResult = ByteArray(20)

        stream.read(hashResult, 0, 20)

        val hashStream = ByteArrayOutputStream()

        //EMV Book 2, page 67, table 15

        //Header not included in hash

        //EMV Book 2, page 67, table 15

        //Header not included in hash
        hashStream.write(signedDataFormat.toInt())
        hashStream.write(hashAlgorithmIndicator.toInt())
        hashStream.write(iccDynamicDataLength)
        hashStream.write(iccDynamicNumber, 0, iccDynamicNumber.size)
        hashStream.write(padding, 0, padding.size)
        hashStream.write(terminalDynamicData, 0, terminalDynamicData.size)
        //Trailer not included in hash

        //Trailer not included in hash
        var sha1Result: ByteArray? = null
        sha1Result = try {
            RSAHelper.calculateSHA1(hashStream.toByteArray())
        } catch (ex: NoSuchAlgorithmException) {
            LogUtils.e("SHA-1 hash algorithm not available")
            return false
        }
        if (!Arrays.equals(sha1Result, hashResult)) {
            LogUtils.e("Hash is not valid")
            return false
        }

        val trailer = stream.read().toByte()

        if (trailer != 0xbc.toByte()) {
            LogUtils.e("Trailer != 0xbc")
            return false
        }

        return true

    }
}