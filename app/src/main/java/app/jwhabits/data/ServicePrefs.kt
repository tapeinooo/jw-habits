package app.jwhabits.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 奉仕の設定。基本の立場と、目標時間（端末の設定に持つ）。 */
class ServicePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _role = MutableStateFlow(
        runCatching { ServiceRole.valueOf(prefs.getString(KEY_ROLE, null) ?: "") }.getOrDefault(ServiceRole.REGULAR_PIONEER)
    )
    private val _annual = MutableStateFlow(prefs.getInt(KEY_ANNUAL, DEFAULT_ANNUAL_GOAL))
    private val _monthly = MutableStateFlow(prefs.getInt(KEY_MONTHLY, DEFAULT_MONTHLY_GOAL))

    val role: StateFlow<ServiceRole> = _role
    val annualGoal: StateFlow<Int> = _annual
    val monthlyGoal: StateFlow<Int> = _monthly

    fun setRole(value: ServiceRole) {
        prefs.edit().putString(KEY_ROLE, value.name).apply()
        _role.value = value
    }

    fun setAnnualGoal(hours: Int) {
        val v = hours.coerceAtLeast(0)
        prefs.edit().putInt(KEY_ANNUAL, v).apply()
        _annual.value = v
    }

    fun setMonthlyGoal(hours: Int) {
        val v = hours.coerceAtLeast(0)
        prefs.edit().putInt(KEY_MONTHLY, v).apply()
        _monthly.value = v
    }

    companion object {
        const val DEFAULT_ANNUAL_GOAL = 600
        const val DEFAULT_MONTHLY_GOAL = 30
        private const val KEY_ROLE = "service_role"
        private const val KEY_ANNUAL = "service_annual_goal"
        private const val KEY_MONTHLY = "service_monthly_goal"
    }
}
