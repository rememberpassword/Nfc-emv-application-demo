package com.rmp.emvengine.common

import java.math.BigInteger
import java.security.MessageDigest

object RSAHelper {

    fun decipher(dataEncipher: ByteArray, modulus: ByteArray, exponent: ByteArray): ByteArray{

        var dataBytes = dataEncipher
        var expBytes = exponent
        var modBytes = modulus
        val inBytesLength = dataBytes.size
        if (expBytes[0] >= 0x80.toByte()) {
            //Prepend 0x00 to modulus
            val tmp = ByteArray(expBytes.size + 1)
            tmp[0] = 0x00.toByte()
            System.arraycopy(expBytes, 0, tmp, 1, expBytes.size)
            expBytes = tmp
        }
        if (modBytes[0] >= 0x80.toByte()) {
            //Prepend 0x00 to modulus
            val tmp = ByteArray(modBytes.size + 1)
            tmp[0] = 0x00.toByte()
            System.arraycopy(modBytes, 0, tmp, 1, modBytes.size)
            modBytes = tmp
        }
        if (dataBytes[0] >= 0x80.toByte()) {
            //Prepend 0x00 to signed data to avoid that the most significant bit is interpreted as the "signed" bit
            val tmp = ByteArray(dataBytes.size + 1)
            tmp[0] = 0x00.toByte()
            System.arraycopy(dataBytes, 0, tmp, 1, dataBytes.size)
            dataBytes = tmp
        }
        val exp = BigInteger(expBytes)
        val mod = BigInteger(modBytes)
        val data = BigInteger(dataBytes)
        var result = data.modPow(exp, mod).toByteArray()
        if (result.size == inBytesLength + 1 && result[0] == 0x00.toByte()) {
            //Remove 0x00 from beginning of array
            val tmp = ByteArray(inBytesLength)
            System.arraycopy(result, 1, tmp, 0, inBytesLength)
            result = tmp
        }
        return result

    }

    fun calculateSHA1(data: ByteArray): ByteArray{
        val sha1 = MessageDigest.getInstance("SHA-1")
        return sha1.digest(data)
    }

}