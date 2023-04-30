// SPDX-License-Identifier: CC0-1.0

plugins {
    `java-library`
    signing
    `maven-publish`
    id("io.freefair.lombok") version "8.0.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    checkstyle
}

group = "de.richardliebscher.mdf4j"
version = "0.1.0-SNAPSHOT"

java {
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.4.1")
    testRuntimeOnly("org.slf4j:jul-to-slf4j:2.0.7")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.7")
}

tasks.withType<Javadoc>().configureEach {
    exclude("**/.*PackageGateway\\.java")
    options {
        (this as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

checkstyle {
    toolVersion = "10.10.0"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    systemProperty("java.util.logging.config.file", "${projectDir.absolutePath}/src/test/resources/logging.properties")
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

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
}

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
    this.repositories {
        create("OSSRH") {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_TOKEN"))
        }
    }
}