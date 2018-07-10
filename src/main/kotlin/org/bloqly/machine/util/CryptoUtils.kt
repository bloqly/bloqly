package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
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

// TODO change it to using Schnorr https://github.com/sipa/bips/blob/bip-schnorr/bip-schnorr.mediawiki
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

    fun hash(inputs: Array<ByteArray>): ByteArray {

        ByteArrayOutputStream().use { bos ->

            for (input in inputs) {
                bos.write(input)
            }

            return MessageDigest.getInstance(SHA_256)
                .digest(bos.toByteArray())
        }
    }

    fun hash(input: ByteArray): ByteArray {

        return MessageDigest.getInstance(SHA_256).digest(input)
    }

    fun hash(input: String): ByteArray {

        return hash(input.toByteArray())
    }

    fun digestTransactions(transactions: List<Transaction>): ByteArray {

        val bos = ByteArrayOutputStream()

        transactions
            .sortedBy { it.id }
            .forEach { bos.write(it.signature) }

        return hash(bos.toByteArray())
    }

    fun digestVotes(votes: List<Vote>): ByteArray {

        val bos = ByteArrayOutputStream()

        votes
            .sortedBy { it.id.validatorId }
            .forEach { bos.write(it.signature) }

        return hash(bos.toByteArray())
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

    fun verifyTransaction(tx: Transaction): Boolean {

        val dataToVerify = Bytes.concat(
            tx.spaceId.toByteArray(),
            tx.origin.toByteArray(),
            tx.destination.toByteArray(),
            tx.value,
            tx.referencedBlockId.toByteArray(),
            tx.transactionType.name.toByteArray(),
            EncodingUtils.longToBytes(tx.timestamp)
        )

        val txHash = hash(tx.signature)

        val transactionId = txHash.encode16()

        if (transactionId != tx.id) {
            return false
        }

        return verify(
            message = hash(dataToVerify),
            signature = tx.signature,
            publicKey = tx.publicKey.decode16()
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

        val dataHash = hash(dataToVerify)

        val publicKey = vote.publicKey.decode16()

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
}
