import io.freefair.gradle.plugins.lombok.tasks.Delombok

// SPDX-License-Identifier: CC0-1.0

plugins {
    `java-library`
    signing
    `maven-publish`
    id("io.freefair.lombok") version "8.13"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("net.researchgate.release") version "3.0.2"
    checkstyle
    //id("net.ltgt.errorprone") version "3.1.0"
}

group = "de.richardliebscher.mdf4j"
version = project.property("version")!!

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets.create("jmh") {
    // Add access to test traces
    runtimeClasspath += sourceSets.test.get().output

    java.setSrcDirs(listOf("src/jmh/java"))
}

sourceSets.create("cli") {
    java.setSrcDirs(listOf("src/cli/java"))
}

sourceSets.create("example") {
    java.setSrcDirs(listOf("src/example/java"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.5")
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testRuntimeOnly("org.slf4j:jul-to-slf4j:2.0.9")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")

    "exampleImplementation"(project)

    "jmhImplementation"(project)
    "jmhImplementation"("org.openjdk.jmh:jmh-core:1.37")
    "jmhAnnotationProcessor"("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    "cliImplementation"(project)
    "cliImplementation"("org.apache.parquet:parquet-common:1.13.1")
    "cliImplementation"("org.apache.parquet:parquet-encoding:1.13.1")
    "cliImplementation"("org.apache.parquet:parquet-column:1.13.1")
    "cliImplementation"("org.apache.parquet:parquet-hadoop:1.13.1")
    "cliImplementation"("org.apache.hadoop:hadoop-common:3.3.6")
    "cliImplementation"("org.apache.hadoop:hadoop-mapreduce-client-core:3.3.6")
    //errorprone("com.google.errorprone:error_prone_core:2.18.0")
}

plugins.withType<JavaPlugin>().configureEach {
    configure<JavaPluginExtension> {
        modularity.inferModulePath.set(true)
    }
}

tasks.withType<Javadoc>().configureEach {
    exclude("**/.*PackageGateway\\.java")
    options {
        (this as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

tasks.withType<Delombok>().configureEach {
    enabled = false
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

//signing {
//    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
//
//    sign(publishing.publications["mavenJava"])
//}

//javadoc {
//    if (JavaVersion.current().isJava9Compatible()) {
//        options.addBooleanOption("html5", true)
//    }
//}

nexusPublishing {
    this.repositories {
        create("OSSRH") {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_TOKEN"))
        }
    }
}

release {
    tagTemplate.set("v\$version")
    failOnCommitNeeded.set(false)

    preTagCommitMessage.set("Prepare release ")
    tagCommitMessage.set("Release ")
    newVersionCommitMessage.set("Set next version ")

    git {
        requireBranch.set("main")
    }
}