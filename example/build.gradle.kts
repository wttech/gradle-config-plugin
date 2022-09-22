plugins {
    id("com.wttech.config")
}

config {
    task {
        group("general") {
            prop("infra") {
                value.set("aws")
                options("aws", "gcp", "az")
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
                value.set("admin")

            }
            prop("aemProxyPassword") {
                value.set("admin")
            }
            listProp("aemPackages") {
                value.set(listOf("a", "b", "c"))
            }
        }
    }
}