package altline.appliance.ui.mapper

import altline.appliance.common.Body
import altline.appliance.fabric.Bedsheet
import altline.appliance.fabric.Fabric
import altline.appliance.fabric.Rag
import altline.appliance.fabric.Towel
import altline.appliance.fabric.clothing.*
import altline.appliance.measure.sumOf
import altline.appliance.substance.CommonSubstanceTypes
import altline.appliance.substance.SubstanceType
import altline.appliance.ui.resources.get
import altline.appliance.ui.resources.strings
import altline.appliance.ui.util.optionalDecimal
import altline.appliance.washing.CommonDetergents
import altline.appliance.washing.laundry.CommonFabricSofteners
import altline.appliance.washing.laundry.washCycle.phase.*

class StringMapper {

    fun mapSubstanceTypeName(substanceType: SubstanceType): String {
        return when (substanceType) {
            CommonSubstanceTypes.WATER -> strings["substanceType_water"]
            CommonSubstanceTypes.MUD -> strings["substanceType_mud"]
            CommonSubstanceTypes.COFFEE -> strings["substanceType_coffee"]
            CommonSubstanceTypes.KETCHUP -> strings["substanceType_ketchup"]
            CommonSubstanceTypes.CRUDE_OIL -> strings["substanceType_crudeOil"]

            CommonDetergents.ULTIMATE_DETERGENT -> "${strings["power_ultimate"]} ${strings["substanceType_detergent_partial"]}"
            CommonDetergents.STRONG_DETERGENT -> "${strings["power_strong"]} ${strings["substanceType_detergent_partial"]}"
            CommonDetergents.BASIC_DETERGENT -> "${strings["power_basic"]} ${strings["substanceType_detergent_partial"]}"
            CommonDetergents.MILD_DETERGENT -> "${strings["power_mild"]} ${strings["substanceType_detergent_partial"]}"
            CommonDetergents.WEAK_DETERGENT -> "${strings["power_weak"]} ${strings["substanceType_detergent_partial"]}"
            CommonDetergents.BARELY_DETERGENT -> "${strings["power_barely"]} ${strings["substanceType_detergent_partial"]}"
            CommonDetergents.USELESS_DETERGENT -> "${strings["power_useless"]} ${strings["substanceType_detergent_partial"]}"

            CommonFabricSofteners.ULTIMATE_SOFTENER -> "${strings["power_ultimate"]} ${strings["substanceType_softener_partial"]}"
            CommonFabricSofteners.STRONG_SOFTENER -> "${strings["power_strong"]} ${strings["substanceType_softener_partial"]}"
            CommonFabricSofteners.BASIC_SOFTENER -> "${strings["power_basic"]} ${strings["substanceType_softener_partial"]}"
            CommonFabricSofteners.MILD_SOFTENER -> "${strings["power_mild"]} ${strings["substanceType_softener_partial"]}"
            CommonFabricSofteners.WEAK_SOFTENER -> "${strings["power_weak"]} ${strings["substanceType_softener_partial"]}"
            CommonFabricSofteners.BARELY_SOFTENER -> "${strings["power_barely"]} ${strings["substanceType_softener_partial"]}"
            CommonFabricSofteners.USELESS_SOFTENER -> "${strings["power_useless"]} ${strings["substanceType_softener_partial"]}"

            else -> ""
        }
    }

    fun mapLaundryName(body: Body): String {
        return when (body) {
            is Sock -> strings["laundryName_sock"]
            is Shirt -> strings["laundryName_shirt"]
            is Pants -> strings["laundryName_pants"]
            is Dress -> strings["laundryName_dress"]
            is Rag -> strings["laundryName_rag"]
            is Towel -> strings["laundryName_towel"]
            is Bedsheet -> strings["laundryName_bedsheet"]
            is Clothing -> strings["laundryName_clothing"]
            is Fabric -> strings["laundryName_fabric"]
            else -> strings["laundryName_body"]
        }
    }

    fun mapClothingSize(size: Clothing.Size): String {
        return when (size) {
            Clothing.Size.XXS -> strings["clothingSize_XXS"]
            Clothing.Size.XS -> strings["clothingSize_XS"]
            Clothing.Size.S -> strings["clothingSize_S"]
            Clothing.Size.M -> strings["clothingSize_M"]
            Clothing.Size.L -> strings["clothingSize_L"]
            Clothing.Size.XL -> strings["clothingSize_XL"]
            Clothing.Size.XXL -> strings["clothingSize_XXL"]
        }
    }

    fun mapPhaseName(phase: CyclePhase): String {
        return buildString {
            append(
                when (phase) {
                    is FillPhase -> strings["cyclePhase_fill"]
                    is WashPhase -> strings["cyclePhase_wash"]
                    is DrainPhase -> strings["cyclePhase_drain"]
                    is SpinPhase -> strings["cyclePhase_spin"]
                    else -> ""
                }
            )
            if (phase is FillPhase) {
                if (phase.sections.size == 1) {
                    append(" - ${mapFillSectionName(phase.sections.first()).lowercase()}")
                } else {
                    append(" (${phase.sections.sumOf { it.fillToAmount }.optionalDecimal()})")
                }
            }
            if (phase is SpinPhase && !phase.disabled) {
                append(" (${phase.sections.maxOf { it.params.spinSpeed }.optionalDecimal()})")
            }
        }
    }

    fun mapFillSectionName(section: FillPhase.Section): String {
        return buildString {
            append(
                when (section) {
                    is FillPhase.PreWashFillSection -> strings["cyclePhase_fill_prewash"]
                    is FillPhase.DetergentFillSection -> strings["cyclePhase_fill_detergent"]
                    is FillPhase.SoftenerFillSection -> strings["cyclePhase_fill_softener"]
                    else -> ""
                }
            )
            append(" (${section.fillToAmount})")
        }
    }
}