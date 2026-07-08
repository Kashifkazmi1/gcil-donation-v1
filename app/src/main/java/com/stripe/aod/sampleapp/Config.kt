package com.stripe.aod.sampleapp

class Config {
    companion object {
        const val TAG = "AOD_SampleApp"

        /**
         * The Stripe Terminal Location that the M2 reader is registered to.
         * Get this from your Stripe Dashboard: Settings -> Terminal -> Locations.
         * It looks like "tml_XXXXXXXXXXXX".
         *
         * You MUST replace this before the app can connect to a physical reader.
         */
        const val LOCATION_ID = "tml_GW5WuQUcquU3cU"
    }
}