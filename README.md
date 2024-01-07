## Introduction
`minikube-maven-extension` is a maven extension for exporting minikube docker environment variables to the maven process.

## Generated properties

`minikube-maven-extension` generates the following properties:

- `DOCKER_TLS_VERIFY`
- `MINIKUBE_ACTIVE_DOCKERD`
- `DOCKER_HOST`
- `DOCKER_CERT_PATH`

Basically, they are all generated from running the below command:
```bash
minikube docker-env
```

### what if there is no minikube installed?

If you don't have minikube installed, `minikube-maven-extension` will just print a warning message and continue the build process. 

### what if minikube is not running?

If you don't have minikube running, `minikube-maven-extension` will just print a warning message and continue the build process. 

## Usage

1. Declare the extension in your pom.xml as below:

```xml
<!--...-->
<build>
    <extensions>
        <extension>
            <groupId>com.dadaer.maven</groupId>
            <artifactId>minikube-maven-extension</artifactId>
            <version>1.0.0</version>
        </extension>
    </extensions>
    <!--...-->
</build>
<!--...-->
```

2. Use the generated properties

For example, the below code snippet is used to build and deploy artifacts to Minikube using JKube.

```xml

<plugin>
    <groupId>org.eclipse.jkube</groupId>
    <artifactId>kubernetes-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <!-- Here, we used the properties generated from minikube-maven-extension -->
        <dockerHost>${DOCKER_HOST}</dockerHost>
        <certPath>${DOCKER_CERT_PATH}</certPath>
        <images>
            <image>
                <name>
                    dadaer.com/${project.groupId}/${project.artifactId}:${project.version}
                </name>
            </image>
        </images>
    </configuration>
    <executions>
        <execution>
            <id>k8s-build</id>
            <goals>
                <goal>build</goal>
                <goal>resource</goal>
            </goals>
            <phase>package</phase>
        </execution>
        <execution>
            <id>k8s-deploy</id>
            <goals>
                <goal>apply</goal>
            </goals>
            <phase>install</phase>
        </execution>
    </executions>
</plugin>

```
