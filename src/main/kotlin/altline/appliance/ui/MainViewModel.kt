package altline.appliance.ui

import altline.appliance.audio.SoundClip
import altline.appliance.common.Body
import altline.appliance.common.RefreshPeriod
import altline.appliance.common.SpeedModifier
import altline.appliance.data.World
import altline.appliance.measure.Volume
import altline.appliance.measure.Volume.Companion.liters
import altline.appliance.measure.repeatPeriodically
import altline.appliance.substance.MutableSubstance
import altline.appliance.substance.SubstanceConsistency.ThickLiquid
import altline.appliance.substance.SubstanceConsistency.ThinLiquid
import altline.appliance.substance.SubstanceType
import altline.appliance.ui.component.laundry.LaundryListItemUi
import altline.appliance.ui.component.laundry.LaundryPanelUi
import altline.appliance.ui.component.washer.WasherPanelUi
import altline.appliance.ui.component.washerInfo.InfoPanelUi
import altline.appliance.ui.mapper.LaundryMapper
import altline.appliance.ui.mapper.WasherInfoMapper
import altline.appliance.ui.mapper.WasherMapper
import altline.appliance.util.wrapAround
import altline.appliance.washing.CommonDetergents
import altline.appliance.washing.laundry.SlottedDispenser
import altline.appliance.washing.laundry.washCycle.PreWashCapable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.nacular.measured.units.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(
    private val world: World,
    private val laundryMapper: LaundryMapper,
    private val washerMapper: WasherMapper,
    private val washerInfoMapper: WasherInfoMapper,
    private val soundPlayer: WasherSoundPlayer
) {

    var uiState by mutableStateOf<MainUiState?>(null)
        private set

    // Dispatcher types other than unconfined cause issues, might need more investigation
    private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)

    private var selectedLaundryItem: Body? = null

    private val washer get() = world.washer

    private val loadedLaundry: Set<Body>
        get() = washer.load

    private val potentialLaundry: Set<Body>
        get() = world.laundry.minus(loadedLaundry)

    private var selectedAdditive: SubstanceType = CommonDetergents.BASIC_DETERGENT

    init {
        uiState = MainUiState(
            laundryPanel = getLaundryPanel(),
            washerPanel = getWasherPanel(),
            infoPanel = getInfoPanel(),
            dispenserTray = null
        )
        coroutineScope.launch {
            delay(1000)
            repeatPeriodically(RefreshPeriod) {
                updateData()
                soundPlayer.updateSounds(washer)
            }
        }
    }

    private fun updateData() {
        uiState = uiState?.copy(
            laundryPanel = getLaundryPanel(),
            washerPanel = getWasherPanel(),
            infoPanel = getInfoPanel()
        )
    }

    private fun updateWasherData() {
        uiState = uiState?.copy(
            washerPanel = getWasherPanel(),
            infoPanel = getInfoPanel()
        )
    }

    private fun getLaundryPanel(): LaundryPanelUi {
        return laundryMapper.mapToLaundryPanel(
            potentialLaundry = potentialLaundry,
            loadedLaundry = loadedLaundry,
            selectedLaundryItem = selectedLaundryItem,
            doorLocked = washer.doorLocked,
            onItemClick = this::selectLaundryItem,
            onItemDoubleClick = { transferLaundryItem(it, selectNext = false) },
            onTransferClick = { selectedLaundryItem?.let { transferLaundryItem(it, selectNext = true) } }
        )
    }

    private fun getWasherPanel(): WasherPanelUi {
        return washerMapper.mapToWasherPanel(
            washer,
            onDispenserClick = this::openDispenser,
            onSelectNextCycle = this::selectNextCycle,
            onPowerOnOff = this::togglePower,
            onStartPause = this::toggleCycleRun,
            onPreWashClick = this::togglePreWash,
            onTempUp = this::increaseTemperature,
            onTempDown = this::decreaseTemperature,
            onRpmUp = this::increaseSpinSpeed,
            onRpmDown = this::decreaseSpinSpeed
        )
    }

    private fun getInfoPanel(): InfoPanelUi {
        return washerInfoMapper.mapToInfoPanel(
            washer = washer,
            speedSettings = listOf(1f, 10f, 20f, 50f),
            selectedSpeed = SpeedModifier,
            onSpeedClick = this::selectSpeed
        )
    }

    private fun selectSpeed(speedModifier: Float) {
        SpeedModifier = speedModifier
    }

    private fun selectLaundryItem(item: Body) {
        selectedLaundryItem = if (selectedLaundryItem == item) null else item
        updateData()
    }

    private fun transferLaundryItem(item: Body, selectNext: Boolean) {
        selectedLaundryItem = if (selectNext) findNextSelection(item) else item
        if (item in washer.load) washer.unload(item)
        else washer.load(item)
        updateData()
    }

    private fun openDispenser() {
        soundPlayer.playClip(SoundClip.DispenserOpen)
        washer.openDispenserTray()
        updateDispenser()
    }

    private fun closeDispenser() {
        soundPlayer.playClip(SoundClip.DispenserClose)
        uiState = uiState?.copy(dispenserTray = null)
        washer.closeDispenserTray()
    }

    private fun addDispenserAdditive(slot: SlottedDispenser.Tray.Slot, amount: Measure<Volume>) {
        if (slot.additive.amount == slot.capacity) return

        soundPlayer.playClip(
            when (selectedAdditive.consistency) {
                ThinLiquid -> SoundClip.ThinLiquidAdd
                ThickLiquid -> SoundClip.ThickLiquidAdd
            }
        )
        slot.fill(
            MutableSubstance(
                type = selectedAdditive,
                amount = amount,
                temperature = world.ambientTemperature
            )
        )
        updateDispenser()
    }

    private fun removeDispenserAdditive(slot: SlottedDispenser.Tray.Slot, amount: Measure<Volume>) {
        if (slot.additive.amount == 0 * liters) return

        soundPlayer.playClip(
            when (slot.additive.largestPart.type.consistency) {
                ThinLiquid -> SoundClip.ThinLiquidRemove
                ThickLiquid -> SoundClip.ThickLiquidRemove
            }
        )
        slot.empty(amount)
        updateDispenser()
    }

    private fun selectAdditive(substanceType: SubstanceType) {
        this.selectedAdditive = substanceType
        updateDispenser()
    }

    private fun updateDispenser() {
        uiState = uiState?.copy(
            dispenserTray = washerMapper.mapDispenserTray(
                tray = washer.dispenserTray,
                selectedAdditive = selectedAdditive,
                onAdditiveAdd = this::addDispenserAdditive,
                onAdditiveRemove = this::removeDispenserAdditive,
                onAdditivePick = this::selectAdditive,
                onCloseTray = this::closeDispenser
            )
        )
    }

    private fun selectNextCycle(reverse: Boolean) {
        soundPlayer.playClip(
            if (washer.poweredOn) SoundClip.CycleSelect
            else SoundClip.CycleSelectOff
        )
        with(washer) {
            val currentCycleIndex = washCycles.indexOf(selectedCycle)
            val nextCycleIndex = kotlin.run {
                if (reverse) currentCycleIndex - 1
                else currentCycleIndex + 1
            }.wrapAround(washCycles.indices)

            selectedCycle = washCycles[nextCycleIndex]
        }
        updateWasherData()
    }

    private fun togglePower() {
        soundPlayer.playClip(
            if (washer.poweredOn) SoundClip.PowerOff
            else SoundClip.PowerOn
        )
        washer.togglePower()
        updateWasherData()
    }

    private fun toggleCycleRun() {
        soundPlayer.playClip(
            if (washer.poweredOn) {
                if (washer.running && !washer.paused) SoundClip.OptionNegative
                else SoundClip.OptionPositive
            } else SoundClip.LargeButtonPress
        )
        washer.toggleCycleRun()
        updateWasherData()
    }

    private fun togglePreWash() {
        val success = washer.togglePreWash()
        updateWasherData()
        soundPlayer.playClip(
            if (washer.poweredOn) {
                if (success) {
                    when ((washer.selectedCycle as? PreWashCapable)?.preWash) {
                        true -> SoundClip.OptionHigh
                        false -> SoundClip.OptionLow
                        null -> SoundClip.OptionDenied
                    }
                } else SoundClip.OptionDenied
            } else SoundClip.LargeButtonPress
        )
    }

    private fun increaseTemperature() {
        val success = washer.increaseTemperature()
        updateWasherData()
        soundPlayer.playClip(
            if (washer.poweredOn) {
                if (success) SoundClip.OptionHigh
                else SoundClip.OptionDenied
            } else SoundClip.SmallButtonPress
        )
    }

    private fun decreaseTemperature() {
        val success = washer.decreaseTemperature()
        updateWasherData()
        soundPlayer.playClip(
            if (washer.poweredOn) {
                if (success) SoundClip.OptionLow
                else SoundClip.OptionDenied
            } else SoundClip.SmallButtonPress
        )
    }

    private fun increaseSpinSpeed() {
        val success = washer.increaseSpinSpeed()
        updateWasherData()
        soundPlayer.playClip(
            if (washer.poweredOn) {
                if (success) SoundClip.OptionHigh
                else SoundClip.OptionDenied
            } else SoundClip.SmallButtonPress
        )
    }

    private fun decreaseSpinSpeed() {
        val success = washer.decreaseSpinSpeed()
        updateWasherData()
        soundPlayer.playClip(
            if (washer.poweredOn) {
                if (success) SoundClip.OptionLow
                else SoundClip.OptionDenied
            } else SoundClip.SmallButtonPress
        )
    }

    /**
     * Finds the laundry item that should receive the selection after the given item is transferred.
     * **Must be called before the transfer is made.**
     */
    private fun findNextSelection(item: Body): Body? {
        val uiPotentialLaundry = uiState!!.laundryPanel.potentialLaundry
        val uiLoadedLaundry = uiState!!.laundryPanel.loadedLaundry
        var owningList: List<LaundryListItemUi> = emptyList()
        val listItem = uiPotentialLaundry.find { it.id == item.id }
            ?.also { owningList = uiPotentialLaundry }
            ?: uiLoadedLaundry.find { it.id == item.id }
                ?.also { owningList = uiLoadedLaundry }
        val index = owningList.indexOf(listItem)
        val nextSelectedIndex = when {
            owningList.size == 1 -> null
            index == owningList.size - 1 -> index - 1
            else -> index + 1
        }
        val nextSelectedId = nextSelectedIndex?.let { owningList[nextSelectedIndex].id }
        return nextSelectedId?.let { world.laundry.find { it.id == nextSelectedId } }
    }
}