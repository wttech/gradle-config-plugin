plugins {
    id("com.wttech.config")
}

config {
    define {
        valueSaveVisible()
        labelAbbrs("aem")

        group("general") {
            prop("infra") {
                value("aws")
                options("local", "aws", "gcp", "az", "vagrant")
            }
            prop("envType") {
                options("afe_single", "aem_single", "aem_multi")
            }
        }
        group("local") {
            label("Local Env")
            visible { value("infra") == name }

            prop("monitoringEnabled") {
                options("true", "false")
                checkbox()
            }
        }
        group("aws_afe_single") {
            label("Remote Env")
            visible { "${value("infra")}_${value("envType")}" == name }

            prop("env") {
                value("kp")
            }
            prop("envMode") {
                options("dev", "stg", "prod")
                enabled { otherValue("env") == "kp" }
            }
            prop("aemInstancePassword") {
                valueDynamic { "${otherValue("env")}-pass" }
            }
            prop("aemProxyPassword") {
                value("admin")
            }
            listProp("aemPackages") {
                values("a", "b", "c")
            }
        }
        group("app") {
            prop("mavenArgs") {
                value("-DskipTests")
            }
        }
        group("test") {
            prop("percyToken")
            prop("percyEnabled") {
                checkbox()
            }
        }
    }
}