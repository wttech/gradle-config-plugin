package com.wttech.gradle.config

import org.gradle.api.provider.Provider

interface Value<T: Any> {

    val value: Provider<T>
}