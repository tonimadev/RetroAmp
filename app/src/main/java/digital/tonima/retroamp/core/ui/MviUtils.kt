package digital.tonima.retroamp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/**
 * A utility component to handle one-time UI effects in an MVI architecture.
 *
 * @param effectFlow The flow of effects from the ViewModel.
 * @param onConsume The intent to dispatch to the ViewModel to consume the effect.
 * @param onEffect The callback to handle the effect (e.g., show a Snackbar).
 */
@Composable
fun <T> LaunchedUiEffectHandler(
    effectFlow: Flow<T?>,
    onConsume: () -> Unit,
    onEffect: (T) -> Unit
) {
    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            if (effect != null) {
                onEffect(effect)
                onConsume()
            }
        }
    }
}
