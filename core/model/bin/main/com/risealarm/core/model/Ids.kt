package com.risealarm.core.model

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class AlarmId(val value: String)

@Serializable
@JvmInline
value class SessionId(val value: String)

@Serializable
@JvmInline
value class MissionId(val value: String)

