package io.hellopulumi.lab04

/** EC2 부팅 시 실행되는 cloud-init 스크립트 (Amazon Linux 2023) */
object UserData {

    fun nginx(stackName: String): String = """
        #!/bin/bash
        set -euxo pipefail
        dnf install -y nginx
        cat > /usr/share/nginx/html/index.html <<'HTML'
        <h1>Hello from Pulumi + Kotlin</h1>
        <p>stack: $stackName</p>
        HTML
        systemctl enable --now nginx
    """.trimIndent()
}
