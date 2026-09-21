package com.myhitbodedut

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Les accès que l'application demande, leur état, et les écrans système pour
 * les accorder.
 *
 * L'état se lit d'ici plutôt que de l'écran des réglages : l'écran principal
 * en a besoin lui aussi, pour dire en une ligne ce qui manque sans qu'on ait
 * à ouvrir quoi que ce soit.
 */
object Permissions {

    /**
     * Avant Android 13, les notifications ne se demandaient pas : elles
     * étaient accordées à l'installation. Rien à réclamer, donc.
     */
    fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun isGranted(context: Context, autorisation: Autorisation): Boolean = when (autorisation) {
        Autorisation.NOTIFICATIONS -> hasNotifications(context)
        Autorisation.BATTERIE -> isBatteryUnrestricted(context)
        Autorisation.OVERLAY -> canDrawOverlays(context)
        Autorisation.USAGE -> hasUsageAccess(context)
    }

    /** Ce qui manque, dans l'ordre, pour cette édition. */
    fun missing(context: Context): List<Autorisation> =
        PermissionRules.manquantes(BuildConfig.HAS_GATE) { isGranted(context, it) }

    /**
     * L'écran système des notifications de l'application. Android cesse
     * d'afficher la demande après deux refus : sans ce recours, le bouton
     * « Accorder » ne ferait plus rien du tout.
     */
    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun overlayIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:" + context.packageName)
    )

    @Suppress("DEPRECATION")
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun isBatteryUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** En édition légère il n'y a pas de barrière, donc rien à accorder. */
    fun allGranted(context: Context): Boolean =
        !BuildConfig.HAS_GATE || (canDrawOverlays(context) && hasUsageAccess(context))
}
