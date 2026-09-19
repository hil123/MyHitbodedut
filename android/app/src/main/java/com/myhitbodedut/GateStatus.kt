package com.myhitbodedut

import android.content.Context
import java.time.LocalDate

/** Pourquoi la barrière est active, ou pourquoi elle ne l'est pas. */
enum class GateState {
    /** Tout est en place : ouvrir une application choisie déclenchera l'écran. */
    ARMED,
    MISSING_PERMISSIONS,
    DISABLED,
    NO_APPS_SELECTED,
    ALREADY_DONE,
    EXEMPT_DAY,
    SNOOZED,
    OUTSIDE_WINDOW
}

/**
 * Source unique de vérité sur l'état de la barrière.
 * Le service s'en sert pour décider, l'écran principal pour l'expliquer.
 */
object GateStatus {

    fun evaluate(context: Context): GateState = when {
        !Permissions.allGranted(context) -> GateState.MISSING_PERMISSIONS
        !Store.isEnabled(context) -> GateState.DISABLED
        Store.blockedApps(context).isEmpty() -> GateState.NO_APPS_SELECTED
        Store.isDone(context) -> GateState.ALREADY_DONE
        Store.isExempt(context, LocalDate.now()) -> GateState.EXEMPT_DAY
        Store.isSnoozed(context) -> GateState.SNOOZED
        !Store.inWindow(context) -> GateState.OUTSIDE_WINDOW
        else -> GateState.ARMED
    }

    fun isArmed(context: Context): Boolean = evaluate(context) == GateState.ARMED

    /** Le service doit-il tourner ? Oui dès que les réglages sont bons, même hors fenêtre. */
    fun shouldRunService(context: Context): Boolean =
        Store.isEnabled(context) && Permissions.allGranted(context)

    fun titleOf(state: GateState): Int = when (state) {
        GateState.ARMED -> R.string.gate_armed_title
        GateState.MISSING_PERMISSIONS -> R.string.gate_permissions_title
        GateState.DISABLED -> R.string.gate_disabled_title
        GateState.NO_APPS_SELECTED -> R.string.gate_no_apps_title
        GateState.ALREADY_DONE -> R.string.gate_done_title
        GateState.EXEMPT_DAY -> R.string.gate_exempt_title
        GateState.SNOOZED -> R.string.gate_snoozed_title
        GateState.OUTSIDE_WINDOW -> R.string.gate_window_title
    }

    fun detailOf(context: Context, state: GateState): String = when (state) {
        GateState.ARMED -> context.getString(
            R.string.gate_armed_detail,
            Store.blockedApps(context).size,
            Store.formatMinutes(Store.windowEnd(context))
        )
        GateState.MISSING_PERMISSIONS -> context.getString(R.string.gate_permissions_detail)
        GateState.DISABLED -> context.getString(R.string.gate_disabled_detail)
        GateState.NO_APPS_SELECTED -> context.getString(R.string.gate_no_apps_detail)
        GateState.ALREADY_DONE -> context.getString(R.string.gate_done_detail)
        GateState.EXEMPT_DAY -> context.getString(R.string.gate_exempt_detail)
        GateState.SNOOZED -> context.getString(R.string.gate_snoozed_detail)
        GateState.OUTSIDE_WINDOW -> context.getString(
            R.string.gate_window_detail,
            Store.formatMinutes(Store.windowStart(context)),
            Store.formatMinutes(Store.windowEnd(context))
        )
    }

    /** Vert quand la barrière veille, orange quand une action est attendue, neutre sinon. */
    fun colorOf(state: GateState): Int = when (state) {
        GateState.ARMED, GateState.ALREADY_DONE -> R.color.brand
        GateState.MISSING_PERMISSIONS, GateState.NO_APPS_SELECTED -> R.color.warning
        else -> R.color.muted
    }
}
