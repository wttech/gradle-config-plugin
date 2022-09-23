plugins {
    id("com.wttech.config")
}

config {
    task {
        group("general") {
            prop("infra") {
                value("aws")
                options("aws", "gcp", "az")
            }
            prop("envType") {
                options.set(listOf("afe_single", "aem_single", "aem_multi"))
            }
            prop("test") {
                value("abc")
            }
        }


        group("aws_afe_single") {
            label.set("Remote environment")
            visible { name == "${value("infra")}_${value("envType")}" }

            prop("env") {
                value("kp")
            }
            prop("envMode") {
                options("dev", "stg", "prod")
                enabled { otherValue("env") == "kp" }
            }
            prop("aemInstancePassword") {
                mutate {
                    otherValue("env")?.toString()
                }
            }
            prop("aemProxyPassword") {
                value("admin")
            }
            listProp("aemPackages") {
                value.set(listOf("a", "b", "c"))
            }
        }
    }
}