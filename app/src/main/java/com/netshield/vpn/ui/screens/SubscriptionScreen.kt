package com.netshield.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netshield.vpn.ui.theme.NetShieldColors

/**
 * Subscription plan picker. Wiring to Google Play Billing (billing-ktx, already added
 * as a dependency) or a custom backend is intentionally left for the next step per
 * your request — this screen is fully functional UI with a single callback,
 * onPlanSelected, ready to hand off to a BillingRepository.
 */
data class Plan(val id: String, val title: String, val price: String, val perDay: String, val badge: String? = null)

private val plans = listOf(
    Plan("1m", "۱ ماهه", "۹۹,۰۰۰ تومان", "۳,۳۰۰ تومان/روز"),
    Plan("3m", "۳ ماهه", "۲۴۹,۰۰۰ تومان", "۲,۷۰۰ تومان/روز", badge = "محبوب"),
    Plan("12m", "۱۲ ماهه", "۷۹۹,۰۰۰ تومان", "۲,۲۰۰ تومان/روز", badge = "بیشترین صرفه")
)

@Composable
fun SubscriptionScreen(onPlanSelected: (Plan) -> Unit = {}) {
    var selected by remember { mutableStateOf(plans[1].id) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NetShieldColors.Background)
            .padding(20.dp)
    ) {
        Text("ارتقا به پرمیوم", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("بدون محدودیت حجم، سرورهای پرسرعت و پشتیبانی از همه پروتکل‌ها", color = NetShieldColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        plans.forEach { plan ->
            val isSelected = plan.id == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NetShieldColors.Surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) NetShieldColors.Accent else NetShieldColors.Divider,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { selected = plan.id }
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            plan.badge?.let {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NetShieldColors.Accent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(it, color = Color.Black, fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp))
                                }
                            }
                        }
                        Text(plan.perDay, color = NetShieldColors.TextSecondary, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
                    }
                    Text(plan.price, color = NetShieldColors.Accent, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { plans.find { it.id == selected }?.let(onPlanSelected) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NetShieldColors.Accent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("ادامه و پرداخت", color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}
