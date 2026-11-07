package com.punitkumar.gruhkharch.domain.model

data class Member(
    val userId: String = "",
    val name: String = "",
    val role: MemberRole = MemberRole.FAMILY_MEMBER,
    val color: String = "#8B5E3C",
    val joinedAt: Long = System.currentTimeMillis(),
    // False for people added by name only (they don't use the app); their userId is "local_<uuid>".
    val hasAccount: Boolean = true
)

enum class MemberRole(val displayName: String) {
    OWNER("Owner"),
    FAMILY_MEMBER("Family Member")
}
