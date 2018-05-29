package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.sec.SECNamedCurves
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

    private val LOG = LoggerFactory.getLogger(CryptoUtils::class.simpleName)

    private val sha256Digest: MessageDigest = MessageDigest.getInstance("SHA-256")

    private val generator: ECKeyPairGenerator = ECKeyPairGenerator()

    private const val CURVE_NAME = "secp256k1"

    private val CURVE = SECNamedCurves.getByName(CURVE_NAME)

    private val DOMAIN = ECDomainParameters(
            CURVE.curve,
            CURVE.g,
            CURVE.n,
            CURVE.h
    )

    init {
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")

        val keygenParams = ECKeyGenerationParameters(DOMAIN, secureRandom)

        generator.init(keygenParams)
    }

    fun generatePrivateKey(): ByteArray {

        val keypair = generator.generateKeyPair()

        val params = keypair.private as ECPrivateKeyParameters

        return params.d.toByteArray()
    }

    fun getPublicFor(privateKey: ByteArray): ByteArray {

        return CURVE.g.multiply(BigInteger(privateKey)).getEncoded(true)
    }

    fun digest(inputs: Array<ByteArray>): ByteArray {

        ByteArrayOutputStream().use { bos ->

            for (input in inputs) {
                bos.write(input)
            }

            return sha256Digest.digest(bos.toByteArray())
        }
    }

    fun digest(input: ByteArray): ByteArray {

        return sha256Digest.digest(input)
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
        val privateKeyParams = ECPrivateKeyParameters(BigInteger(privateKey), DOMAIN)

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
                transaction.space.toByteArray(),
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

    fun verifyVote(validator: Account, vote: Vote): Boolean {

        val dataToVerify = Bytes.concat(
                vote.id.validatorId.toByteArray(),
                vote.id.space.toByteArray(),
                EncodingUtils.longToBytes(vote.id.height),
                vote.blockId.toByteArray(),
                EncodingUtils.longToBytes(vote.timestamp)
        )

        val dataHash = digest(dataToVerify)

        val publicKey = decodeFromString16(validator.publicKey)

        return verify(dataHash, vote.signature, publicKey)
    }

    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {

        try {
            val decoder = ASN1InputStream(signature)

            val seq = decoder.readObject() as DLSequence
            val r = seq.getObjectAt(0) as ASN1Integer
            val s = seq.getObjectAt(1) as ASN1Integer

            decoder.close()

            val q = CURVE.curve.decodePoint(publicKey)
            val pubParams = ECPublicKeyParameters(q, DOMAIN)

            val signer = ECDSASigner()
            signer.init(false, pubParams)

            return signer.verifySignature(message, r.positiveValue, s.positiveValue)
        } catch (e: Exception) {
            LOG.error(String.format("Could not verify signature for message %s", String(message)), e)
            return false
        }
    }
}
