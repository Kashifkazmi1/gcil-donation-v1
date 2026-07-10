package com.stripe.aod.sampleapp.activity

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

class TapCardActivity : Activity() {

  private val TAG = "TapCardActivity"
  private var nfcAdapter: NfcAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    nfcAdapter = NfcAdapter.getDefaultAdapter(this)

    if (nfcAdapter == null) {
      // Device does not support NFC
      Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_LONG).show()
      Log.e(TAG, "NFC adapter is null - device lacks NFC hardware")
      // show UI fallback
      return
    }

    if (!nfcAdapter!!.isEnabled) {
      Toast.makeText(this, "Please enable NFC in settings", Toast.LENGTH_LONG).show()
      Log.w(TAG, "NFC is disabled on the device")
      return
    }
  }

  override fun onResume() {
    super.onResume()
    // Enable reader mode with flags appropriate for your card types. Skip NDEF check can speed things.
    try {
      nfcAdapter?.enableReaderMode(
        this,
        { tag -> onTagDiscovered(tag) },
        NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
        null
      )
      Log.d(TAG, "Reader mode enabled")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to enable reader mode", e)
      runOnUiThread {
        Toast.makeText(this, "Failed to initialize NFC reader: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  override fun onPause() {
    super.onPause()
    try {
      nfcAdapter?.disableReaderMode(this)
      Log.d(TAG, "Reader mode disabled")
    } catch (e: Exception) {
      Log.w(TAG, "Error disabling reader mode", e)
    }
  }

  private fun onTagDiscovered(tag: Tag) {
    Log.d(TAG, "Tag discovered: ${tag.id?.joinToString(",")}")
    // Handle connection on background thread
    Thread {
      var iso: IsoDep? = null
      try {
        iso = IsoDep.get(tag)
        if (iso == null) {
          Log.e(TAG, "IsoDep is null for tag -> unsupported tag tech")
          showToastOnUi("Unsupported card type")
          return@Thread
        }
        iso.connect()
        // Set timeouts if necessary: iso.timeout = 5000
        // send APDUs / perform handoff protocol here...
        // Example: val response = iso.transceive(cmd)
        Log.d(TAG, "Connected to IsoDep; perform handoff")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to connect to handoff reader", e)
        showToastOnUi("Failed to connect handoff reader: ${e.localizedMessage ?: e.message}")
      } finally {
        try { iso?.close() } catch (_: Exception) {}
      }
    }.start()
  }

  private fun showToastOnUi(message: String) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
  }
}
