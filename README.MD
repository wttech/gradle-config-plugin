[![WTT logo](docs/wtt-logo.png)](https://www.wundermanthompson.com/service/technology)

[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)

# Gradle Config Plugin

## About

Defining configuration for Gradle via DSL with ability to provide values with multiple input modes:

- GUI 
- CLI interactive
- CLI non-interactive

```kotlin
plugins {
    id("com.wttech.config")
}

config {
    task {
        group("general") {
            prop("infra") {
                value.set("aws")
                options.set(listOf("aws", "gcp", "az"))
            }
            prop("envType") {
                value.set("afe_single2")
                options.set(listOf())
            }
        }
        group("aws_afe_single") {
            visible { name == "${value("infra")}_${value("envType")}" }

            prop("env") {
                value.set("kp")
            }
            prop("envMode") {
                value.set("dev")
            }
            prop("aemInstancePassword") {

            }
            prop("aemProxyPassword") {

            }
        }
    }
}
```

## Debugging tests

To debug plugin source code while:

* running functional tests, append `--debug-jvm -Porg.gradle.testkit.debug=true`.
* project using plugin, append `--no-daemon -Dorg.gradle.debug=true`.

Gradle will stop for a moment and wait until remote connection at port 5005 will be established from e.g IDE.

## Contributing

Issues reported or pull requests created will be very appreciated. 

1. Fork plugin source code using a dedicated GitHub button.
2. Do code changes on a feature branch created from *develop* branch.
3. Create a pull request with a base of *develop* branch.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
