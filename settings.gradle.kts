pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
//            url = uri("C:\\Users\\Sazzad_PTSL\\Desktop\\MyBL SDK\\mybl-network-measurement-sdk\\network-sdk\\build\\outputs\\aar")
//            url = uri("/Users/shebleredwan/Desktop/network_measurement_event_base_sdk/network-sdk/build/outputs/aar")

//            credentials {
//                username ="primetechsoltutions"
//                password = "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"
//            }
        }

//        maven {
//            url = uri("/Users/shebleredwan/Desktop/network_measurement_event_base_sdk/network-sdk/build/outputs/aar")
////            url = uri("/Users/shebleredwan/Desktop/Work/Network SDK/MYBL-Event-Base-SDK-Test-v1.0.0-Updated")
//
//        }

    }
}

rootProject.name = "NetworkSdk_Event"
include(":app")
include(":network-sdk")
