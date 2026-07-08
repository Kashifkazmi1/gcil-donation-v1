package com.stripe.aod.sampleapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.stripe.aod.sampleapp.R
import com.stripe.aod.sampleapp.databinding.FragmentHomeBinding
import com.stripe.aod.sampleapp.model.MainViewModel
import com.stripe.aod.sampleapp.utils.launchAndRepeatWithViewLifecycle
import com.stripe.aod.sampleapp.utils.navOptions
import com.stripe.aod.sampleapp.utils.setThrottleClickListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import kotlinx.coroutines.flow.filter

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val viewModel: MainViewModel by activityViewModels()

    private var selectedPresetView: TextView? = null
    private var selectedAmountCents: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentHomeBinding.bind(view)

        launchAndRepeatWithViewLifecycle {
            viewModel.readerConnectStatus.collect {
                if (it == ConnectionStatus.CONNECTED) {
                    viewBinding.connectionStatus.text = getString(R.string.reader_connected)
                } else {
                    // Reader dropped while we were on this screen: send the person
                    // back to the connect screen instead of letting them try to pay.
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToConnectReaderFragment(),
                        navOptions()
                    )
                }
            }
        }

        launchAndRepeatWithViewLifecycle {
            viewModel.userMessage.filter {
                it.isNotEmpty()
            }.collect { message ->
                Snackbar.make(viewBinding.donateButton, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        val presetButtons = listOf(
            viewBinding.amount25 to 2500,
            viewBinding.amount50 to 5000,
            viewBinding.amount75 to 7500,
            viewBinding.amount100 to 10000,
        )

        presetButtons.forEach { (button, cents) ->
            button.setThrottleClickListener { selectPreset(button, cents, viewBinding) }
        }

        viewBinding.customAmountInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                clearPresetSelection()
            }
        }

        viewBinding.donateButton.setThrottleClickListener {
            val amountCents = resolveAmountCents(viewBinding)

            if (amountCents == null || amountCents <= 0) {
                Snackbar.make(
                    viewBinding.donateButton,
                    getString(R.string.error_fail_to_create_payment_intent),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setThrottleClickListener
            }

            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToTapCardFragment(amountCents),
                navOptions()
            )
        }
    }

    private fun resolveAmountCents(viewBinding: FragmentHomeBinding): Int? {
        val customText = viewBinding.customAmountInput.text?.toString()?.trim().orEmpty()

        return if (customText.isNotEmpty()) {
            customText.toDoubleOrNull()?.let { dollars -> (dollars * 100).toInt() }
        } else {
            selectedAmountCents
        }
    }

    private fun selectPreset(button: TextView, cents: Int, viewBinding: FragmentHomeBinding) {
        selectedPresetView?.isSelected = false
        button.isSelected = true
        selectedPresetView = button
        selectedAmountCents = cents
        viewBinding.customAmountInput.setText("")
    }

    private fun clearPresetSelection() {
        selectedPresetView?.isSelected = false
        selectedPresetView = null
        selectedAmountCents = null
    }
}
