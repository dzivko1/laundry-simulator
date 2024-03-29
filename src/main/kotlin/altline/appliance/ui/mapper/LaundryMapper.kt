package altline.appliance.ui.mapper

import altline.appliance.common.Body
import altline.appliance.fabric.clothing.Clothing
import altline.appliance.measure.Volume.Companion.milliliters
import altline.appliance.measure.sumOf
import altline.appliance.substance.SoakableBody
import altline.appliance.ui.component.laundry.LaundryListItemUi
import altline.appliance.ui.component.laundry.LaundryPanelUi
import altline.appliance.washing.laundry.fresheningPotential
import io.nacular.measured.units.*

class LaundryMapper(
    private val stringMapper: StringMapper
) {

    fun mapToLaundryPanel(
        potentialLaundry: Set<Body>,
        loadedLaundry: Set<Body>,
        selectedLaundryItem: Body?,
        doorLocked: Boolean,
        onItemClick: (Body) -> Unit,
        onItemDoubleClick: (Body) -> Unit,
        onTransferClick: () -> Unit
    ): LaundryPanelUi {
        val bodyToListItem = { item: Body ->
            mapToLaundryListItem(
                item,
                selected = item == selectedLaundryItem,
                onClick = { onItemClick(item) },
                onDoubleClick = { onItemDoubleClick(item) }
            )
        }
        return LaundryPanelUi(
            potentialLaundry = potentialLaundry.map(bodyToListItem),
            loadedLaundry = loadedLaundry.map(bodyToListItem),
            onTransferClick = if (!doorLocked && selectedLaundryItem != null) onTransferClick else null
        )
    }

    private fun mapToLaundryListItem(
        body: Body,
        selected: Boolean,
        onClick: () -> Unit,
        onDoubleClick: () -> Unit
    ): LaundryListItemUi {
        val name = stringMapper.mapLaundryName(body)
        val size = if (body is Clothing) stringMapper.mapClothingSize(body.size) else ""
        val soakRatio = if (body is SoakableBody) body.soakRatio else null
        return LaundryListItemUi(
            id = body.id,
            title = buildString {
                append("$name, ")
                if (size.isNotBlank()) append(size)
                else append(body.volume)
            },
            stainRatio = body.stainRatio,
            soakRatio = soakRatio,
            freshness = mapFreshness(body),
            selected = selected,
            onClick = onClick,
            onDoubleClick = onDoubleClick
        )
    }

    private fun mapFreshness(body: Body): Double {
        val freshnessInfluencers = buildSet {
            addAll(body.stainSubstance.parts)
            if (body is SoakableBody) {
                addAll(body.soakedSubstance.parts)
            }
        }.filterNot { it.type.evaporates }

        val amount = freshnessInfluencers.sumOf { it.amount }

        val fresheningPotential = freshnessInfluencers.fold(0.0) { acc: Double, part ->
            val ratio = part.amount / amount
            val effectivePower = part.type.fresheningPotential * ratio
            acc + effectivePower
        }

        return run {
            fresheningPotential * (body.stainSubstance.amount / (0.1 * milliliters)) / (body.volume / (100 * milliliters))
        }.coerceAtMost(1.0)
    }
}