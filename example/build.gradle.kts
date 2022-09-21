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
                value.set("afe_single")
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