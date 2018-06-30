package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

object CryptoUtils {

    private val log = LoggerFactory.getLogger(CryptoUtils::class.simpleName)

    private val generator: ECKeyPairGenerator = ECKeyPairGenerator()

    private const val CURVE_NAME = "secp256k1"

    private const val SHA_256 = "SHA-256"

    init {
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")

        val keygenParams = ECKeyGenerationParameters(getDomain(getCurve()), secureRandom)

        generator.init(keygenParams)
    }

    private fun getCurve(): X9ECParameters {
        return SECNamedCurves.getByName(CURVE_NAME)
    }

    private fun getDomain(curve: X9ECParameters): ECDomainParameters {
        return ECDomainParameters(
            curve.curve,
            curve.g,
            curve.n,
            curve.h
        )
    }

    fun generatePrivateKey(): ByteArray {

        val keypair = generator.generateKeyPair()

        val params = keypair.private as ECPrivateKeyParameters

        return params.d.toByteArray()
    }

    fun getPublicFor(privateKey: ByteArray): ByteArray {

        return getCurve().g.multiply(BigInteger(privateKey)).getEncoded(true)
    }

    fun digest(inputs: Array<ByteArray>): ByteArray {

        ByteArrayOutputStream().use { bos ->

            for (input in inputs) {
                bos.write(input)
            }

            return MessageDigest.getInstance(SHA_256)
                .digest(bos.toByteArray())
        }
    }

    fun digest(input: ByteArray): ByteArray {

        return MessageDigest.getInstance(SHA_256).digest(input)
    }

    fun digest(input: String): ByteArray {

        return digest(input.toByteArray())
    }

    fun digestTransactions(transactions: List<Transaction>): ByteArray {

        val bos = ByteArrayOutputStream()

        transactions
            .sortedBy { it.id }
            .forEach { bos.write(it.signature) }

        return digest(bos.toByteArray())
    }

    fun digestVotes(votes: List<Vote>): ByteArray {

        val bos = ByteArrayOutputStream()

        votes
            .sortedBy { it.id.validatorId }
            .forEach { bos.write(it.signature) }

        return digest(bos.toByteArray())
    }

    fun sign(privateKey: ByteArray, input: ByteArray): ByteArray {

        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privateKeyParams = ECPrivateKeyParameters(BigInteger(privateKey), getDomain(getCurve()))

        signer.init(true, privateKeyParams)

        val signature = signer.generateSignature(input)

        val bos = ByteArrayOutputStream()
        val sequenceGenerator = DERSequenceGenerator(bos)
        sequenceGenerator.addObject(ASN1Integer(signature[0]))
        sequenceGenerator.addObject(ASN1Integer(signature[1]))
        sequenceGenerator.close()

        return bos.toByteArray()
    }

    fun verifyTransaction(transaction: Transaction): Boolean {

        val dataToVerify = Bytes.concat(
            transaction.spaceId.toByteArray(),
            transaction.origin.toByteArray(),
            transaction.destination.toByteArray(),
            transaction.value,
            transaction.referencedBlockId.toByteArray(),
            transaction.transactionType.name.toByteArray(),
            EncodingUtils.longToBytes(transaction.timestamp)
        )

        val txHash = digest(transaction.signature)

        val transactionId = EncodingUtils.encodeToString16(txHash)

        if (transactionId != transaction.id) {
            return false
        }

        return verify(
            message = digest(dataToVerify),
            signature = transaction.signature,
            publicKey = decodeFromString16(transaction.publicKey)
        )
    }

    fun verifyVote(vote: Vote): Boolean {

        val dataToVerify = Bytes.concat(
            vote.id.validatorId.toByteArray(),
            vote.id.spaceId.toByteArray(),
            EncodingUtils.longToBytes(vote.id.height),
            vote.id.voteType.name.toByteArray(),
            vote.blockId.toByteArray(),
            EncodingUtils.longToBytes(vote.timestamp)
        )

        val dataHash = digest(dataToVerify)

        val publicKey = decodeFromString16(vote.publicKey)

        return verify(dataHash, vote.signature, publicKey)
    }

    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {

        try {
            val decoder = ASN1InputStream(signature)

            val seq = decoder.readObject() as DLSequence
            val r = seq.getObjectAt(0) as ASN1Integer
            val s = seq.getObjectAt(1) as ASN1Integer

            decoder.close()

            val curve = getCurve()
            val q = curve.curve.decodePoint(publicKey)
            val pubParams = ECPublicKeyParameters(q, getDomain(curve))

            val signer = ECDSASigner()
            signer.init(false, pubParams)

            return signer.verifySignature(message, r.positiveValue, s.positiveValue)
        } catch (e: Exception) {
            log.error(e.message, e)
            return false
        }
    }

    fun getSyncBlockId(lastBlock: Block): String {
        val newHeight = lastBlock.height + 1

        val blockIdBytes = Bytes.concat(
            lastBlock.spaceId.toByteArray(),
            EncodingUtils.longToBytes(newHeight),
            lastBlock.id.toByteArray()
        )

        val blockIdBytesHash = digest(blockIdBytes)

        return EncodingUtils.encodeToString16(blockIdBytesHash)
    }
}
