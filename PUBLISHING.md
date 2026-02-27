## Publishing to Maven Central 

### Prerequisites
* You have a **GitHub account**
* You own a **website + domain**
* You want to publish a Java/Kotlin library

---

## Setup Sonatype


### Sonatype Central Portal Account

Go to: [https://central.sonatype.com](https://central.sonatype.com)

* Sign in using your **GitHub account**
* This links your GitHub identity to publishing permissions

This replaces the old OSSRH JIRA ticket process.

### Verify Domain Ownership

Maven Central requires that you control the namespace you're publishing under.

Example:

| Domain          | Group ID        |
| --------------- | --------------- |
| `mycompany.com` | `com.mycompany` |
| `example.org`   | `org.example`   |

### How to verify domain in Sonatype

Inside **Central Portal**:

* Go to **Namespaces**
* Click **Add Namespace**
* Enter your domain (e.g., `mycompany.com`)
* Choose verification method: uploading a verification file to your website

Once approved, you can publish under:

```
com.mycompany
```

## Prepare GPG Key

Maven Central requires **artifact signing**.

**MacOS**

Add to the .zshrc

```
## Homebrew
export PATH="/opt/homebrew/bin:$PATH"
## Setting to allow pinentry-mac to work
export GPG_TTY=$(tty)
```

Then install GPG

```
brew install pinentry-mac
brew install gnupg
which pinentry-mac
```

Setup the pinentry-mac helper editing `~/.gnupg/gpg-agent.conf` and adding the following
This is to allow the GPG command prompt

```
pinentry-program /usr/local/bin/pinentry-mac
```
**Linux**

```
sudo apt install gnupg
```

**Windows**

Install Gpg4win

### Generate a key

```
gpg --full-generate-key
```

Choose:

* RSA and RSA
* 4096 bits
* No expiration (or 2y)
* Use the SAME email as your Sonatype account

### List Keys

```
gpg --list-secret-keys --keyid-format LONG
```

Example output:

```
sec   rsa4096/ABCDEF1234567890 2025-01-01
```

Your key ID:

```
ABCDEF1234567890
```

### Export Public Key

```
gpg --armor --export ABCDEF1234567890
```

Copy output.

### Publish Public Key to Keyserver

```
gpg --keyserver keyserver.ubuntu.com --send-keys ABCDEF1234567890
```

OR

```
gpg --keyserver keys.openpgp.org --send-keys ABCDEF1234567890
```

This is required so Maven Central can verify signatures.

---

## Configure Maven

Add to the pom.xml:

### Required Metadata

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>3.11.2</version>
            <configuration>
                <doclint>-missing</doclint>
            </configuration>
            <executions>
                <execution>
                    <id>createJavadocs</id>
                    <phase>package</phase>
                    <goals>
                        <goal>jar</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <version>0.7.0</version>
            <extensions>true</extensions>
            <configuration>
                <publishingServerId>central</publishingServerId>

            </configuration>
        </plugin>
  </plugins>
</build>
<groupId>com.mycompany</groupId>
<artifactId>my-library</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>

<name>My Library</name>
<description>My awesome library</description>
<url>https://mycompany.com</url>

<licenses>
  <license>
    <name>Apache License 2.0</name>
    <url>https://www.apache.org/licenses/LICENSE-2.0</url>
  </license>
</licenses>

<developers>
  <developer>
    <id>yourid</id>
    <name>Your Name</name>
    <email>your@email.com</email>
  </developer>
</developers>

<scm>
  <connection>scm:git:git://github.com/youruser/my-library.git</connection>
  <developerConnection>scm:git:ssh://github.com:youruser/my-library.git</developerConnection>
  <url>https://github.com/youruser/my-library</url>
</scm>
```

To avoid the deploy for certain packages

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>injected-central-publishing</id>
                    <phase>none</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### The signing plugin

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-gpg-plugin</artifactId>
      <version>3.1.0</version>
      <executions>
        <execution>
          <id>sign-artifacts</id>
          <phase>verify</phase>
          <goals>
            <goal>sign</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```



### Add Distribution Management

From Central Portal → generate publishing token.

Then add to `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>central</id>
    <username>YOUR_USERNAME</username>
    <password>YOUR_TOKEN</password>
  </server>
</servers>
```

And in `pom.xml`:

```xml
<distributionManagement>
  <repository>
    <id>central</id>
    <url>https://central.sonatype.com/api/v1/publisher</url>
  </repository>
</distributionManagement>
```


### Publish

```
mvn clean deploy
```

## Moving  the keys somewhere else

Go on the right dir:
* Windows: `%APPDATA%\gnupg`
* Linux/MacOs: `~/.gnupg`

### Export

```
gpg --export --armor > public-keys.asc
# The key will be required
gpg --export-secret-keys --armor > private-keys.asc
gpg --export-ownertrust > trustdb.txt
```

Copy all files to the target machine.
Delete them ASAP (after the import)

### Import

```
gpg --import     public-keys.asc
gpg --import     private-keys.asc
gpg --import-ownertrust     trustdb.txt
```
