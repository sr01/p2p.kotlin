package com.sr01.p2p.identity

object NameIdentitySerializer : IdentitySerializer<NameIdentity>, IdentityDeserializer<NameIdentity> {

    private const val DELIMITER = "#!#"

    override fun serialize(identity: NameIdentity): String {
        return identity.name + DELIMITER + identity.description + DELIMITER + identity.host + DELIMITER + identity.port
    }

    override fun deserialize(data: String): NameIdentity {
        val fields = data.split(DELIMITER)
        return NameIdentity(fields[0], fields[1], fields[2], fields[3].toInt())
    }
}
