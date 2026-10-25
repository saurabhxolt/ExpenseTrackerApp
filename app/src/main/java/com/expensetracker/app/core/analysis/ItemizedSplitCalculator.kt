package com.expensetracker.app.core.analysis

import java.util.Locale
import java.util.UUID

object ItemizedSplitCalculator {

    data class ItemizedDish(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val price: Double,
        val consumers: Set<String> = emptySet()
    )

    data class PersonBreakdown(
        val personName: String,
        val baseAmount: Double,
        val taxShare: Double,
        val tipShare: Double,
        val totalAmount: Double,
        val itemsSummary: List<String>
    )

    data class ItemizedSplitResult(
        val subtotal: Double,
        val taxAmount: Double,
        val tipAmount: Double,
        val grandTotal: Double,
        val breakdown: List<PersonBreakdown>
    )

    fun calculateSplit(
        dishes: List<ItemizedDish>,
        allMembers: List<String>,
        taxPercentage: Double = 0.0,
        taxDirectAmount: Double? = null,
        tipPercentage: Double = 0.0,
        tipDirectAmount: Double? = null
    ): ItemizedSplitResult {
        val subtotal = dishes.sumOf { it.price }

        val totalTax = taxDirectAmount ?: (subtotal * (taxPercentage / 100.0))
        val totalTip = tipDirectAmount ?: (subtotal * (tipPercentage / 100.0))
        val grandTotal = subtotal + totalTax + totalTip

        val baseShares = mutableMapOf<String, Double>()
        val itemsMap = mutableMapOf<String, MutableList<String>>()

        allMembers.forEach { member ->
            baseShares[member] = 0.0
            itemsMap[member] = mutableListOf()
        }

        for (dish in dishes) {
            val assignedConsumers = if (dish.consumers.isEmpty()) allMembers.toSet() else dish.consumers
            val count = assignedConsumers.size
            if (count > 0 && dish.price > 0) {
                val perPerson = dish.price / count
                for (consumer in assignedConsumers) {
                    baseShares[consumer] = (baseShares[consumer] ?: 0.0) + perPerson
                    val shareDesc = if (count == 1) dish.name else "${dish.name} (1/$count)"
                    itemsMap.getOrPut(consumer) { mutableListOf() }.add(shareDesc)
                }
            }
        }

        val breakdown = allMembers.map { member ->
            val base = baseShares[member] ?: 0.0
            val ratio = if (subtotal > 0.0) base / subtotal else 0.0
            val tax = totalTax * ratio
            val tip = totalTip * ratio
            val total = base + tax + tip

            PersonBreakdown(
                personName = member,
                baseAmount = round2(base),
                taxShare = round2(tax),
                tipShare = round2(tip),
                totalAmount = round2(total),
                itemsSummary = itemsMap[member] ?: emptyList()
            )
        }

        return ItemizedSplitResult(
            subtotal = round2(subtotal),
            taxAmount = round2(totalTax),
            tipAmount = round2(totalTip),
            grandTotal = round2(grandTotal),
            breakdown = breakdown
        )
    }

    fun formatWhatsAppReport(
        title: String,
        result: ItemizedSplitResult,
        paidBy: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("🍕 Itemized Bill Split: ${title.ifBlank { "Group Dining" }}\n")
        sb.append(
            String.format(
                Locale.getDefault(),
                "Subtotal: ₹%,.2f | Tax: ₹%,.2f | Tip: ₹%,.2f\nGrand Total: ₹%,.2f\n",
                result.subtotal,
                result.taxAmount,
                result.tipAmount,
                result.grandTotal
            )
        )
        if (!paidBy.isNullOrBlank()) {
            sb.append("Paid by: $paidBy\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        for (p in result.breakdown) {
            sb.append(
                String.format(
                    Locale.getDefault(),
                    "• %s: ₹%,.2f (Base: ₹%,.2f, Tax+Tip: ₹%,.2f)\n",
                    p.personName,
                    p.totalAmount,
                    p.baseAmount,
                    p.taxShare + p.tipShare
                )
            )
            if (p.itemsSummary.isNotEmpty()) {
                sb.append("   ↳ Shared: ${p.itemsSummary.joinToString(", ")}\n")
            }
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Calculated via VaultExpense (Offline Vault)")
        return sb.toString()
    }

    private fun round2(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}
