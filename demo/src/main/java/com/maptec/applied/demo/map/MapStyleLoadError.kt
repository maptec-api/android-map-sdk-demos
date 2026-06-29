package com.maptec.applied.demo.map

import android.content.Context
import android.widget.Toast
import com.maptec.applied.demo.R

fun mapStyleLoadUserMessage(context: Context, rawMessage: String): String {
    val message = rawMessage.trim()
    return when {
        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("No address associated with hostname", ignoreCase = true) ->
            context.getString(R.string.map_style_load_error_network)

        message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ->
            context.getString(R.string.map_style_load_error_timeout)

        message.isEmpty() ->
            context.getString(R.string.map_style_load_error_unknown)

        else -> context.getString(R.string.map_style_load_error_generic, message)
    }
}

fun showMapStyleLoadError(context: Context, rawMessage: String) {
    Toast.makeText(
        context,
        mapStyleLoadUserMessage(context, rawMessage),
        Toast.LENGTH_LONG,
    ).show()
}
