package com.devforfun.example.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.widget.Toast


class NetworkState : BroadcastReceiver() {
    companion object {
        private var instance: NetworkState? = null
        public fun init(contex: Context) {
            instance = NetworkState()
            instance!!.mContext = contex
        }

        public fun getInstance(): NetworkState {
            return instance!!
        }
    }

    private lateinit var mContext: Context

    private var connectivityReceiverListener: ConnectivityReceiverListener? = null
    private var connectivityRun: (() -> Unit)? = null
    private var connectivityNotAvailableRun: (() -> Unit)? = null
    private var connectivityAlwaysAvailableRun: (() -> Unit)? = null
    private var connectivityAlwaysNotAvailableRun: (() -> Unit)? = null



    override fun onReceive(context: Context?, intent: Intent?) {
        val isNetworkWork = isInternetAvailable()
        connectivityReceiverListener?.onNetworkConnectionChanged(isNetworkWork)
        if (isInternetAvailable()){
            if(connectivityRun != null){
                connectivityRun!!.invoke()
                connectivityRun = null
            }
            connectivityAlwaysAvailableRun?.invoke()
        }
        else {
            if(connectivityNotAvailableRun != null){
                connectivityNotAvailableRun?.invoke()
                connectivityNotAvailableRun = null
            }
            connectivityAlwaysNotAvailableRun?.invoke()

        }

    }

    fun isInternetAvailable(): Boolean {
        var result = false
        val connectivityManager =
            mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result
    }


    fun runWhenNetworkAvailable(run: () -> Unit): NetworkState {
        connectivityRun = run
        if (isInternetAvailable()) {
            connectivityRun?.invoke()
        }
        return instance!!
    }

    fun runAlwaysWhenNetworkNotAvailable(run: () -> Unit): NetworkState {
        connectivityAlwaysNotAvailableRun = run
        if(!isInternetAvailable()){
            connectivityAlwaysNotAvailableRun?.invoke()
        }
        return instance!!
    }

    fun runAlwaysWhenNetworkStateAvailable(run: () -> Unit): NetworkState {
        connectivityAlwaysAvailableRun = run
        if(!isInternetAvailable()){
            connectivityNotAvailableRun?.invoke()
        }
        return instance!!
    }


    fun runWhenNetworkNotAvailable(run: () -> Unit, timeMillis : Long): NetworkState{
        Handler().postDelayed({
            if(!isInternetAvailable()){
                connectivityNotAvailableRun = null
                connectivityRun = null
                run.invoke()
            }
        }, timeMillis)
        return instance!!
    }

    fun showNetworkNotAvailableMessage(message: String): NetworkState{
        if(!isInternetAvailable()) Toast.makeText(mContext, message, Toast.LENGTH_LONG).show()
        return instance!!
    }

    fun setListener(listener: ConnectivityReceiverListener) {
        connectivityReceiverListener = listener
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }
}