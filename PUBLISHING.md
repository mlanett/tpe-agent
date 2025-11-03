# Publishing Guide

This guide explains how to publish tpe-agent to Maven Central and GitHub Releases.

## One-Time Setup

### 1. Register with Maven Central

1. Go to [central.sonatype.com](https://central.sonatype.com/)
2. Click "Sign In" and authenticate with your GitHub account
3. Verify your namespace:
   - Click your username → "View Namespaces"
   - Add namespace: `io.github.mlanett` (recommended for GitHub users)
   - Or use `mlanett` if you own the domain
   - Follow verification instructions (add a special repository or DNS record)

### 2. Generate PGP Key

You need a PGP key to sign your artifacts:

```bash
# Generate a new key (if you don't have one)
gpg --gen-key
# Follow prompts: use your name and email, choose a passphrase

# List your keys to get the key ID
gpg --list-secret-keys --keyid-format=long

# Export the key in ASCII-armored format
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# The content of private-key.asc is your SIGNING_KEY
# Your passphrase is your SIGNING_PASSWORD
```

### 3. Configure Secrets

#### For GitHub Actions (Automated Publishing)

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

1. **MAVEN_CENTRAL_USERNAME**: Your Maven Central username (from central.sonatype.com)
2. **MAVEN_CENTRAL_PASSWORD**: Your Maven Central password (generate a token at central.sonatype.com → View Account → Generate User Token)
3. **SIGNING_KEY**: Contents of your `private-key.asc` file (entire content including `-----BEGIN PGP PRIVATE KEY BLOCK-----`)
4. **SIGNING_PASSWORD**: Your PGP key passphrase

#### For Local Publishing (Optional)

Add to `~/.gradle/gradle.properties`:

```properties
mavenCentral.username=your-username
mavenCentral.password=your-password-or-token
signing.key=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signing.password=your-pgp-passphrase
```

**Note**: For the signing key in gradle.properties, replace actual newlines with `\n`.

## Publishing a Release

### Option 1: Automated via GitHub (Recommended)

1. Update the version in `build.gradle.kts`:

   ```kotlin
   version = "0.0.3"
   ```

2. Commit and push:

   ```bash
   git add build.gradle.kts
   git commit -m "Release version 0.0.3"
   git push
   ```

3. Create and push a tag:

   ```bash
   git tag v0.0.3
   git push origin v0.0.3
   ```

4. GitHub Actions will automatically:
   - Build the project
   - Sign all artifacts
   - Publish to Maven Central (auto-released)
   - Create a GitHub Release with the agent JAR attached

5. Verify the release:
   - Check GitHub Actions for build status
   - Visit [central.sonatype.com](https://central.sonatype.com/) → Deployments
   - Check GitHub Releases page
   - Within ~10-30 minutes, the artifact will appear on [Maven Central](https://central.sonatype.com/artifact/mlanett/tpe-agent)

### Option 2: Manual Local Publishing

```bash
# Build and publish
./gradlew publish

# This will publish to Maven Central (if credentials are configured)
# You may need to manually create a GitHub Release for the agent JAR
```

## After Publishing

Once published to Maven Central, users can depend on your library without any authentication:

```kotlin
dependencies {
    implementation("mlanett:tpe-agent:0.0.3")
    // For the agent JAR:
    agentConfiguration("mlanett:tpe-agent:0.0.3:agent@jar")
}
```

## Troubleshooting

### "401 Unauthorized" from Maven Central

- Verify your username and password/token are correct
- Ensure you're using a User Token (not your account password)

### "Failed to sign"

- Check that `SIGNING_KEY` includes the full key with headers
- Verify `SIGNING_PASSWORD` matches your key's passphrase
- Make sure there are no extra spaces or newlines in the secrets

### "Namespace not verified"

- Complete the namespace verification in central.sonatype.com
- For `io.github.mlanett`, you may need to create a verification repository

### Publishing succeeds but artifact not visible

- Maven Central sync can take 10-30 minutes
- Check central.sonatype.com → Deployments for status
- First-time publishing may require manual verification

## Reference

- [Maven Central Portal Documentation](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Gradle Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
- [GitHub Actions Setup Java](https://github.com/actions/setup-java)
