package com.expensetracker.app.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupQrSyncEngineTest {

    private val engine = GroupQrSyncEngine()

    @Test
    fun testEncodeAndDecodeShortPayload() {
        val payload = GroupSyncPayload(
            version = 1,
            senderName = "Alice",
            group = GroupSyncData(
                name = "Weekend Outing",
                description = "Picnic",
                emoji = "🧺",
                createdAt = 1729850000000L,
                members = listOf("You", "Bob"),
                expenses = emptyList(),
                settlements = emptyList()
            )
        )

        val encoded = engine.encodeGroupPayload(payload)
        assertTrue(encoded.startsWith("ET_GRP:1:") || encoded.startsWith("ET_GRP_Z:1:"))

        val decoded = engine.decodeGroupPayload(encoded)
        assertNotNull(decoded)
        assertEquals("Alice", decoded!!.senderName)
        assertEquals("Weekend Outing", decoded.group.name)
        assertEquals("🧺", decoded.group.emoji)
        assertEquals(2, decoded.group.members.size)
    }

    @Test
    fun testEncodeAndDecodeCompressedPayloadWithExpensesAndShares() {
        val expenses = listOf(
            ExpenseSyncData(
                title = "Dinner at Britto's",
                totalAmount = 3000.0,
                paidByMember = "You",
                category = "Food & Dining",
                timestamp = 1729851000000L,
                shares = listOf(
                    ShareSyncData("You", 1000.0),
                    ShareSyncData("Bob", 1000.0),
                    ShareSyncData("Charlie", 1000.0)
                )
            ),
            ExpenseSyncData(
                title = "Scooter Rental",
                totalAmount = 1500.0,
                paidByMember = "Bob",
                category = "Travel",
                timestamp = 1729852000000L,
                shares = listOf(
                    ShareSyncData("You", 500.0),
                    ShareSyncData("Bob", 500.0),
                    ShareSyncData("Charlie", 500.0)
                )
            )
        )

        val settlements = listOf(
            SettlementSyncData(
                fromMember = "Charlie",
                toMember = "You",
                amount = 1000.0,
                timestamp = 1729853000000L
            )
        )

        val payload = GroupSyncPayload(
            version = 1,
            senderName = "Alice",
            group = GroupSyncData(
                name = "Goa Vacation 2026",
                description = "Sun and Sand",
                emoji = "🏖️",
                createdAt = 1729850000000L,
                members = listOf("You", "Bob", "Charlie"),
                expenses = expenses,
                settlements = settlements
            )
        )

        val encoded = engine.encodeGroupPayload(payload)
        // Exceeds 250 chars -> compressed with GZIP Base64
        assertTrue("Encoded payload should use GZIP prefix: $encoded", encoded.startsWith("ET_GRP_Z:1:"))

        val decoded = engine.decodeGroupPayload(encoded)
        assertNotNull(decoded)
        assertEquals("Alice", decoded!!.senderName)
        assertEquals("Goa Vacation 2026", decoded.group.name)
        assertEquals(2, decoded.group.expenses.size)
        assertEquals(1, decoded.group.settlements.size)

        val firstExp = decoded.group.expenses[0]
        assertEquals("Dinner at Britto's", firstExp.title)
        assertEquals(3000.0, firstExp.totalAmount, 0.01)
        assertEquals(3, firstExp.shares.size)
    }

    @Test
    fun testIdentityMappingFromSenderPerspectiveToRecipientPerspective() {
        val payload = GroupSyncPayload(
            version = 1,
            senderName = "Alice",
            group = GroupSyncData(
                name = "Dinner Party",
                members = listOf("You", "Bob", "Charlie"),
                expenses = listOf(
                    ExpenseSyncData(
                        title = "Pizza",
                        totalAmount = 900.0,
                        paidByMember = "You", // Alice paid
                        shares = listOf(
                            ShareSyncData("You", 300.0),
                            ShareSyncData("Bob", 300.0),
                            ShareSyncData("Charlie", 300.0)
                        )
                    ),
                    ExpenseSyncData(
                        title = "Drinks",
                        totalAmount = 600.0,
                        paidByMember = "Bob", // Bob paid
                        shares = listOf(
                            ShareSyncData("You", 200.0),
                            ShareSyncData("Bob", 200.0),
                            ShareSyncData("Charlie", 200.0)
                        )
                    )
                ),
                settlements = listOf(
                    SettlementSyncData(fromMember = "Charlie", toMember = "You", amount = 300.0)
                )
            )
        )

        // Bob scans on his phone and declares: "I am Bob"
        val mappedGroup = engine.mapIdentity(payload, myIdentityInGroup = "Bob")

        // In mapped group:
        // Sender's "You" -> "Alice"
        // Bob -> "You"
        assertTrue(mappedGroup.members.contains("You"))
        assertTrue(mappedGroup.members.contains("Alice"))
        assertTrue(mappedGroup.members.contains("Charlie"))

        val pizzaExp = mappedGroup.expenses.first { it.title == "Pizza" }
        assertEquals("Alice", pizzaExp.paidByMember)
        assertEquals("Alice", pizzaExp.shares[0].memberName)
        assertEquals("You", pizzaExp.shares[1].memberName)
        assertEquals("Charlie", pizzaExp.shares[2].memberName)

        val drinksExp = mappedGroup.expenses.first { it.title == "Drinks" }
        assertEquals("You", drinksExp.paidByMember) // Bob paid, so on Bob's phone it's "You"

        val settlement = mappedGroup.settlements[0]
        assertEquals("Charlie", settlement.fromMember)
        assertEquals("Alice", settlement.toMember)
    }
}
