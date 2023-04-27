import java.net.URI

// SPDX-License-Identifier: CC0-1.0

plugins {
    `java-library`
    signing
    `maven-publish`
    id("io.freefair.lombok") version "8.0.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

group = "de.richardliebscher.mdf4j"
//archivesBaseName = "mdf4j"
version = "0.1.0-SNAPSHOT"

java {
    withSourcesJar()
    withJavadocJar()
}

//artifacts {
//    archives(javadocJar, sourcesJar)
//}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

//uploadArchives {
//    repositories {
//        mavenDeployer {
//            beforeDeployment { deployment -> signing.signPom(deployment) }

//            repository(url: "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
//                authentication(userName: System.getenv("OSSRH_USERNAME"), password: System.getenv("OSSRH_TOKEN"))
//            }
//
//            snapshotRepository(url: "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
//                authentication(userName: System.getenv("OSSRH_USERNAME"), password: System.getenv("OSSRH_TOKEN"))
//            }
//
//            pom.project {
//                inceptionYear = "2023"
//                name = project.name
//                packaging = "jar"
//                description = project.description
//
//                url = "https://github.com/R1tschY/mdf4j"
//
//                scm {
//                    connection = "scm:git:https://github.com/R1tschY/mdf4j.git"
//                    url = "https://github.com/R1tschY/mdf4j"
//                    issueManagement {
//                        url = "https://github.com/R1tschY/mdf4j/issues"
//                    }
//                }
//
//                licenses {
//                    license {
//                        name = "Apache-2.0"
//                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
//                    }
//                }
//
//                developers {
//                    developer {
//                        id = "richardliebscher"
//                        name = "Richard Liebscher"
//                        email = "r1tschy@posteo.de"
//                    }
//                }
//            }
//        }
//    }
//}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

//            artifact(tasks["sourcesJar"])
//            artifact(tasks["javadocJar"])

            pom {
                inceptionYear.set("2023")
                name.set(project.name)
                packaging = "jar"
                description.set("MDF4 reader")

                url.set("https://github.com/R1tschY/mdf4j")

                scm {
                    connection.set("scm:git:https://github.com/R1tschY/mdf4j.git")
                    url.set("https://github.com/R1tschY/mdf4j")
                    issueManagement {
                        url.set("https://github.com/R1tschY/mdf4j/issues")
                    }
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("r1tschy")
                        name.set("Richard Liebscher")
                        email.set("r1tschy@posteo.de")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = if (version.toString().endsWith("SNAPSHOT")) {
                URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }

            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_TOKEN")
            }
        }
    }
}

//nexusPublishing {
//    repositories {
//        sonatype {
//            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"))
//            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
//            username = System.getenv("OSSRH_USERNAME")
//            password = System.getenv("OSSRH_TOKEN")
//        }
//    }
//}

signing {
    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))

    sign(publishing.publications["mavenJava"])
}

//javadoc {
//    if (JavaVersion.current().isJava9Compatible()) {
//        options.addBooleanOption("html5", true)
//    }
//}

nexusPublishing {
    repositories {
        create("OSSRH") {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_TOKEN"))
        }
    }
}

//nexusPublishing {
//    repositories {
//        sonatype {
//            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"))
//            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
//            username = System.getenv("OSSRH_USERNAME")
//            password = System.getenv("OSSRH_TOKEN")
//        }
//    }
//}