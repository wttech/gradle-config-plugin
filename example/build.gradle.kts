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
            description("Environment set up directly on current machine")
            visible { value("infra") == name }

            prop("monitoringEnabled") {
                options("true", "false")
                checkbox()
            }
        }
        group("aws_afe_single") {
            label("Remote Env")
            description("Dedicated env for AFE app deployed on AWS infra")
            visible { "${value("infra")}_${value("envType")}" == name }

            prop("env") {
                value("kp")
            }
            prop("envMode") {
                options("dev", "stg", "prod")
                description("Controls AEM run mode")
                enabled { otherValue("env") == "kp" }
            }
            prop("aemInstancePassword") {
                valueDynamic { "${otherValue("env")}-pass" }
                description("Needed to access AEM admin (author & publish)")
            }
            prop("aemProxyPassword") {
                value("admin")
                description("Needed to access AEM dispatcher pages")
            }
            listProp("aemPackages") {
                values("a", "b", "c")
            }
        }
        group("app") {
            description("Application build settings")
            prop("mavenArgs") {
                value("-DskipTests")
            }
        }
        group("test") {
            description("Automated tests execution settings")
            prop("percyToken")
            prop("percyEnabled") {
                checkbox()
            }
        }
    }
}