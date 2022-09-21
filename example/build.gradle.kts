plugins {
    id("com.wttech.config")
}

config {
    task {
        group("general") {
            prop("infra") {

            }
            prop("envType") {

            }
        }
        group("aws_afe_single") {
            visible { name == (value("infra") + value("envType")) }

            prop("env") {

            }
            prop("envMode") {

            }
            prop("aemInstancePassword") {

            }
            prop("aemProxyPassword") {

            }
        }
    }
}