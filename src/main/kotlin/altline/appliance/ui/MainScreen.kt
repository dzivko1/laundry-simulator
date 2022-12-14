package altline.appliance.ui

import altline.appliance.ui.component.VerticalDivider
import altline.appliance.ui.component.laundry.LaundryPanel
import altline.appliance.ui.component.laundry.LaundryPanelUi
import altline.appliance.ui.component.washer.WasherPanel
import altline.appliance.ui.component.washer.WasherPanelUi
import altline.appliance.ui.component.washerInfo.InfoPanel
import altline.appliance.ui.component.washerInfo.InfoPanelUi
import altline.appliance.ui.theme.AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(viewModel: MainViewModel) {
    viewModel.uiState?.run {
        Content(laundryPanel, washerPanel, infoPanel)
    }
}

@Composable
private fun Content(
    laundry: LaundryPanelUi,
    washer: WasherPanelUi,
    info: InfoPanelUi
) {
    var rowSize by remember { mutableStateOf(IntSize.Zero) }

    val minLaundryPanelWidth = 250.dp
    val minInfoPanelWidth = 250.dp

    Row(Modifier.onSizeChanged { rowSize = it }, verticalAlignment = Alignment.CenterVertically) {
        LaundryPanel(laundry, Modifier.requiredWidthIn(min = minLaundryPanelWidth).weight(1f))
        WasherPanel(washer, Modifier.requiredWidthIn(max = rowSize.width.dp - (minLaundryPanelWidth + minInfoPanelWidth)))
        VerticalDivider(thickness = 3.dp)
        InfoPanel(info, Modifier.requiredWidthIn(min = minInfoPanelWidth).weight(1f))
    }
}

@Preview
@Composable
private fun PreviewContent() {
    AppTheme {
        Content(
            LaundryPanelUi.preview(),
            WasherPanelUi.preview(),
            InfoPanelUi.preview()
        )
    }
}
