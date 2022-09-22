plugins {
    id("com.wttech.config")
}

config {
    task {
        group("general") {
            prop("infra") {
                value = "aws"
                options.set(listOf("aws", "gcp", "az"))
            }
            prop("envType") {
                value = "afe_single2"
                options.set(listOf())
            }
        }
        group("aws_afe_single") {
            visible { name == "${value("infra")}_${value("envType")}" }

            prop("env") {
                value = "kp"
            }
            prop("envMode") {
                value = "dev"
            }
            prop("aemInstancePassword") {

            }
            prop("aemProxyPassword") {

            }
        }
    }
}