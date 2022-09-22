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
                options.set(listOf("afe_single", "aem_single", "aem_multi"))
            }
        }
        group("aws_afe_single") {
            label.set("Environment")
            visible { name == "${value("infra")}_${value("envType")}" }

            prop("env") {
                value.set("kp")
            }
            prop("envMode") {
                value.set("dev")
                enabled { otherValue("infra") == "az"  }
            }
            prop("aemInstancePassword") {
                value.set("admin")

                visible { otherValue("infra") == "aws" }
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