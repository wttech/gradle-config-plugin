plugins {
    id("com.wttech.config")
}

config {
    task {
        group("general") {
            prop("infra") {
                text {
                    value.set("aws")
                }
            }
            prop("envType") {
                text {
                    value.set("afe_single")
                }
            }
        }
        group("aws_afe_single") {
            visible { name == "${value("infra")}_${value("envType")}" }

            prop("env") {
                text {
                    value.set("kp")
                }
            }
            prop("envMode") {
                text {
                    value.set("dev")
                }
            }
            prop("aemInstancePassword") {

            }
            prop("aemProxyPassword") {

            }
        }
    }
}