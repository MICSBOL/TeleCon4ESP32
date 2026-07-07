package com.micsbol.telecon4esp32.domain.model

fun ApplicationId.protocolPrefix(): String = when (this) {
    ApplicationId.CONTROL_PANEL -> "RC"
    ApplicationId.RC_VEHICLE_PRO -> "RC"
    ApplicationId.GREENHOUSE -> "GH"
    ApplicationId.SOLAR_POWER -> "SP"
    ApplicationId.SMART_HOME -> "SH"
    ApplicationId.WATER_TANK -> "WT"
    ApplicationId.SMART_DOOR_LOCK -> "DL"
    ApplicationId.SMART_LIGHTING -> "LT"
}
