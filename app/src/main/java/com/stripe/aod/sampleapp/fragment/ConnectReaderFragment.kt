package com.stripe.aod.sampleapp.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.stripe.aod.sampleapp.R
import com.stripe.aod.sampleapp.databinding.FragmentConnectReaderBinding
import com.stripe.aod.sampleapp.model.MainViewModel
import com.stripe.aod.sampleapp.utils.launchAndRepeatWithViewLifecycle
import com.stripe.aod.sampleapp.utils.navOptions
import com.stripe.aod.sampleapp.utils.setThrottleClickListener
import com.stripe.stripeterminal.external.models.ConnectionStatus

class ConnectReaderFragment : Fragment(R.layout.fragment_connect_reader) {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentConnectReaderBinding.bind(view)

        // In case we land back here (e.g. reader disconnected), kick off scanning again.
        viewModel.startDiscovery()

        val deviceHasNfc = requireContext().packageManager
            .hasSystemFeature(PackageManager.FEATURE_NFC)

        if (deviceHasNfc) {
            viewBinding.usePhoneTapToPay.visibility = View.VISIBLE
            viewBinding.usePhoneTapToPay.setThrottleClickListener {
                viewBinding.connectionStatus.text = getString(R.string.reader_connecting)
                viewModel.startPhoneTapToPay()
            }
        }

        launchAndRepeatWithViewLifecycle {
            viewModel.readerConnectStatus.collect { status ->
                if (status == ConnectionStatus.CONNECTED) {
                    viewBinding.connectionStatus.text = getString(R.string.status_reader_connected)
                    findNavController().navigate(
                        ConnectReaderFragmentDirections.actionConnectReaderFragmentToHomeFragment(),
                        navOptions()
                    )
                } else {
                    viewBinding.connectionStatus.text = getString(R.string.reader_connecting)
                }
            }
        }

        launchAndRepeatWithViewLifecycle {
            viewModel.userMessage.collect { message ->
                if (message.isNotEmpty()) {
                    Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearUserMessage()
                }
            }
        }
    }
}
