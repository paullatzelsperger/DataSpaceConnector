plugins {
    `java-library`
}

val jwtVersion: String by project

val nimbusVersion: String by project

dependencies {
    api(project(":data-protocols:ion:ion-core"))
    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id
}

publishing {
    publications {
        create<MavenPublication>("data-protocols.ion-client") {
            artifactId = "data-protocols.ion-client"
            from(components["java"])
        }
    }
}
