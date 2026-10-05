package com.fsck.k9.helper

import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto

object IdentityHelper {
    private val RECIPIENT_TYPES = listOf(
        RecipientType.TO,
        RecipientType.CC,
        RecipientType.X_ORIGINAL_TO,
        RecipientType.DELIVERED_TO,
        RecipientType.X_ENVELOPE_TO,
    )

    /**
     * Find the identity a message was sent to.
     *
     * @param account
     * The account the message belongs to.
     * @param message
     * The message to get the recipients from.
     *
     * @return The identity the message was sent to, or the account's default identity if it
     * couldn't be determined which identity this message was sent to.
     *
     * @see LegacyAccountDto.findIdentity
     */
    @JvmStatic
    fun getRecipientIdentityFromMessage(account: LegacyAccountDto, message: Message): Identity {
        val recipient: Identity? = RECIPIENT_TYPES.asSequence()
            .flatMap { recipientType -> message.getRecipients(recipientType).asSequence() }
            .map { address -> account.findIdentity(address) }
            .filterNotNull()
            .firstOrNull()

        return recipient ?: account.getIdentity(0)
    }

    /**
     * Find the account and identity a message belongs to, for writing a reply or forward from an account that
     * cannot send itself: the identity it was sent to, looking at the recipients in the order
     * [getRecipientIdentityFromMessage] does, or failing that the identity it was sent from (mail in a Sent
     * folder).
     *
     * @param accounts The accounts to look in, in the order to prefer them when several have the address.
     * @return The first match, or `null` if no account has an identity the message was sent to or from.
     */
    @JvmStatic
    fun findAccountIdentityOfMessage(accounts: List<LegacyAccountDto>, message: Message): AccountIdentity? {
        return findAccountIdentity(accounts, message.recipientAddresses() + message.senderAddresses())
    }

    /**
     * Like [findAccountIdentityOfMessage], but for a draft, which belongs to the identity it is written as: the
     * sender is looked at first.
     */
    @JvmStatic
    fun findAccountIdentityOfDraft(accounts: List<LegacyAccountDto>, message: Message): AccountIdentity? {
        return findAccountIdentity(accounts, message.senderAddresses() + message.recipientAddresses())
    }

    private fun findAccountIdentity(accounts: List<LegacyAccountDto>, addresses: Sequence<Address>): AccountIdentity? {
        return addresses
            .mapNotNull { address ->
                accounts.firstNotNullOfOrNull { account ->
                    account.findIdentity(address)?.let { identity -> AccountIdentity(account, identity) }
                }
            }
            .firstOrNull()
    }

    private fun Message.recipientAddresses(): Sequence<Address> {
        return RECIPIENT_TYPES.asSequence().flatMap { recipientType -> getRecipients(recipientType).asSequence() }
    }

    private fun Message.senderAddresses(): Sequence<Address> = from.orEmpty().asSequence()

    data class AccountIdentity(val account: LegacyAccountDto, val identity: Identity)
}
