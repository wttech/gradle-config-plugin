plugins {
    id("com.wttech.config")
}

config {
    define {
        label("GAT configuration")

        valueSaveVisible()
        labelAbbrs("aem")

        group("general") {
            description("Infrastructure and environment type selection")
            prop("infra") {
                value("aws")
                options("local", "aws", "gcp", "az", "vagrant")
            }
            prop("envType") {
                options("afe_single", "aem_single", "aem_multi")
            }
            dynamicProp("domain") { "gat-${otherStringValue("infra")}.wttech.cloud" }
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
        group("remote-aws_afe_single") {
            label("Remote Env")
            description("Dedicated env for AFE app deployed on AWS infra")
            visible { name == "remote-${value("infra")}_${value("envType")}" }

            prop("env") {
                value("kp")
                alphanumeric()
            }
            prop("envMode") {
                options("dev", "stg", "prod")
                description("Controls AEM run mode")
                enabled { otherStringValue("env") == "kp" }
            }
            prop("aemInstancePassword") {
                valueDynamic { otherStringValue("env")?.takeIf { it.isNotBlank() }?.let { "$it-pass" } }
                description("Needed to access AEM admin (author & publish)")
                required()
            }
            prop("aemProxyPassword") {
                value("admin")
                description("Needed to access AEM dispatcher pages")
                required()
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
            dynamicProp("testBaseUrl") {
                when (otherStringValue("infra")) {
                    "local" -> "https://publish.local.gat.com"
                    else -> "https://${otherValue("env")}.${otherValue("domain")}"
                }
            }
        }
    }
}

tasks {
    register("printAemInstancePassword") {
        doLast {
            println(config["aemInstancePassword"])
        }
    }
}