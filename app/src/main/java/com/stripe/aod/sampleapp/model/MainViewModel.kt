package com.stripe.aod.sampleapp.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.aod.sampleapp.Config
import com.stripe.aod.sampleapp.listener.TerminalEventListener
import com.stripe.aod.sampleapp.network.TokenProvider
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.AppsOnDevicesListener
import com.stripe.stripeterminal.external.callable.BluetoothReaderListener
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.EasyConnectConfiguration
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val tokenProvider = TokenProvider(viewModelScope)

    private val _readerConnectStatus: MutableStateFlow<ConnectionStatus> = MutableStateFlow(
        ConnectionStatus.NOT_CONNECTED
    )
    val readerConnectStatus: StateFlow<ConnectionStatus> = _readerConnectStatus.asStateFlow()
    private val _readerPaymentStatus: MutableStateFlow<PaymentStatus> = MutableStateFlow(
        PaymentStatus.NOT_READY
    )
    val readerPaymentStatus: StateFlow<PaymentStatus> = _readerPaymentStatus.asStateFlow()

    /** Readers found nearby while scanning (usually just the one M2). */
    private val _discoveredReaders: MutableStateFlow<List<Reader>> = MutableStateFlow(emptyList())
    val discoveredReaders: StateFlow<List<Reader>> = _discoveredReaders.asStateFlow()

    /** True once we've kicked off a connect attempt on a discovered reader. */
    private val _isConnecting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    /** Live "insert/tap/remove card" style prompts from the reader during a payment. */
    private val _readerDisplayMessage: MutableStateFlow<String> = MutableStateFlow("")
    val readerDisplayMessage: StateFlow<String> = _readerDisplayMessage.asStateFlow()

    private var discoveryTask: Cancelable? = null

    private val _userMessage: MutableStateFlow<String> = MutableStateFlow("")
    val userMessage: StateFlow<String> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                TerminalEventListener.onConnectionStatusChange.collect(::updateConnectStatus)
            }

            launch {
                TerminalEventListener.onPaymentStatusChange.collect(::updatePaymentStatus)
            }
        }
    }

    /**
     * Starts scanning for nearby Stripe Reader M2 devices over Bluetooth.
     * Automatically connects to the first reader it finds.
     */
    fun startDiscovery() {
        if (discoveryTask != null || Terminal.getInstance().connectedReader != null) {
            return
        }

        val discoveryConfig = DiscoveryConfiguration.BluetoothDiscoveryConfiguration(
            isSimulated = false
        )

        discoveryTask = Terminal.getInstance().discoverReaders(
            discoveryConfig,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    _discoveredReaders.update { readers }

                    if (readers.isNotEmpty() && !_isConnecting.value &&
                        Terminal.getInstance().connectedReader == null
                    ) {
                        connectToReader(readers.first())
                    }
                }
            },
            object : Callback {
                override fun onSuccess() {
                    Log.i(Config.TAG, "Discovery finished")
                }

                override fun onFailure(e: TerminalException) {
                    _userMessage.update { e.errorMessage }
                }
            }
        )
    }

    /**
     * Skips the physical reader and turns this phone's own NFC chip into the
     * card reader instead (Stripe Tap to Pay).
     */
    fun startPhoneTapToPay() {
        cancelDiscoveryTask()

        val readerCallback: ReaderCallback = object : ReaderCallback {
            override fun onSuccess(reader: Reader) {
                Log.i(Config.TAG, "Reader [ ${reader.id} ] Connected ")
            }

            override fun onFailure(e: TerminalException) {
                _userMessage.update { e.errorMessage }
            }
        }

        discoveryTask = Terminal.getInstance().easyConnect(
            config = EasyConnectConfiguration.AppsOnDevicesEasyConnectionConfiguration(
                discoveryConfiguration = DiscoveryConfiguration.AppsOnDevicesDiscoveryConfiguration(),
                connectionConfiguration = ConnectionConfiguration.AppsOnDevicesConnectionConfiguration(
                    object : AppsOnDevicesListener {
                        override fun onDisconnect(reason: DisconnectReason) {
                            Log.i(Config.TAG, "onDisconnect: $reason")
                        }

                        override fun onReportReaderEvent(event: ReaderEvent) {
                            Log.i(Config.TAG, "onReportReaderEvent: $event")
                        }
                    }
                ),
            ),
            readerCallback,
        )
    }

    private fun connectToReader(reader: Reader) {
        _isConnecting.update { true }
        cancelDiscoveryTask()

        val readerListener = object : BluetoothReaderListener {
            override fun onReportReaderEvent(event: ReaderEvent) {
                Log.i(Config.TAG, "onReportReaderEvent: $event")
            }

            override fun onRequestReaderInput(options: ReaderInputOptions) {
                _readerDisplayMessage.update { options.toString() }
            }

            override fun onRequestReaderDisplayMessage(displayMessage: ReaderDisplayMessage) {
                _readerDisplayMessage.update { displayMessage.toString() }
            }

            override fun onDisconnect(reason: DisconnectReason) {
                Log.i(Config.TAG, "onDisconnect: $reason")
                _isConnecting.update { false }
            }
        }

        val connectionConfig = ConnectionConfiguration.BluetoothConnectionConfiguration(
            locationId = Config.LOCATION_ID,
            bluetoothReaderListener = readerListener,
        )

        Terminal.getInstance().connectReader(
            reader,
            connectionConfig,
            object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    Log.i(Config.TAG, "Reader [ ${reader.id} ] Connected ")
                    _isConnecting.update { false }
                }

                override fun onFailure(e: TerminalException) {
                    _isConnecting.update { false }
                    _userMessage.update { e.errorMessage }
                }
            }
        )
    }

    fun clearUserMessage() {
        _userMessage.update { "" }
    }

    private fun updateConnectStatus(status: ConnectionStatus) {
        _readerConnectStatus.update { status }
    }

    private fun updatePaymentStatus(status: PaymentStatus) {
        _readerPaymentStatus.update { status }
    }

    private fun cancelDiscoveryTask() {
        discoveryTask?.cancel(object : Callback {
            override fun onSuccess() {
                discoveryTask = null
            }

            override fun onFailure(e: TerminalException) {
                discoveryTask = null
            }
        })
    }

    private fun stopDiscovery() {
        cancelDiscoveryTask()

        Terminal.getInstance().disconnectReader(object : Callback {
            override fun onFailure(e: TerminalException) {
            }

            override fun onSuccess() {
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
