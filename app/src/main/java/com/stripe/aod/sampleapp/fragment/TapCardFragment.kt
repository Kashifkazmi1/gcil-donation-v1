package com.stripe.aod.sampleapp.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.stripe.aod.sampleapp.R
import com.stripe.aod.sampleapp.data.CreatePaymentParams
import com.stripe.aod.sampleapp.databinding.FragmentTapCardBinding
import com.stripe.aod.sampleapp.model.CheckoutViewModel
import com.stripe.aod.sampleapp.model.MainViewModel
import com.stripe.aod.sampleapp.utils.formatCentsToString
import com.stripe.aod.sampleapp.utils.launchAndRepeatWithViewLifecycle
import com.stripe.aod.sampleapp.utils.navOptions
import com.stripe.aod.sampleapp.utils.setThrottleClickListener

class TapCardFragment : Fragment(R.layout.fragment_tap_card) {
    private val checkoutViewModel by viewModels<CheckoutViewModel>()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val args: TapCardFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentTapCardBinding.bind(view)

        viewBinding.amount.text = formatCentsToString(args.amount)

        viewBinding.back.setThrottleClickListener {
            findNavController().navigateUp()
        }

        launchAndRepeatWithViewLifecycle {
            mainViewModel.readerDisplayMessage.collect { message ->
                if (message.isNotEmpty()) {
                    viewBinding.paymentStatus.text = message
                }
            }
        }

        launchAndRepeatWithViewLifecycle {
            checkoutViewModel.paymentCompleted.collect { completed ->
                if (completed) {
                    findNavController().navigate(
                        TapCardFragmentDirections.actionTapCardFragmentToThankYouFragment(),
                        navOptions()
                    )
                    checkoutViewModel.resetPaymentCompleted()
                }
            }
        }

        requestPayment()
    }

    private fun requestPayment() {
        checkoutViewModel.createPaymentIntent(
            CreatePaymentParams(
                amount = args.amount,
                currency = "usd",
                description = "Donation payment",
            )
        ) { failureMessage ->
            Snackbar.make(
                requireView(),
                failureMessage.value.ifEmpty {
                    getString(R.string.error_fail_to_create_payment_intent)
                },
                Snackbar.LENGTH_LONG
            ).show()
            findNavController().navigateUp()
        }
    }
}
