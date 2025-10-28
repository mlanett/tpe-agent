# Publishing Guide

This guide explains how to publish the tpe-agent library to GitHub Packages.

## Prerequisites

1. **GitHub Repository**: Create a public repository at `https://github.com/mlanett/tpe-agent`

2. **GitHub Personal Access Token**:
   - Go to <https://github.com/settings/tokens>
   - Click "Generate new token (classic)"
   - Give it a descriptive name like "tpe-agent publishing"
   - Select the `write:packages` scope (this also includes `read:packages`)
   - Click "Generate token" and copy the token immediately (you won't see it again)

## Configuration

### Option 1: Using gradle.properties (Recommended)

Create a `gradle.properties` file in the project root (it's gitignored) or in `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.token=ghp_your_personal_access_token_here
version=0.0.2
```

### Option 2: Using Environment Variables

Set these environment variables before publishing:

```bash
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=ghp_your_personal_access_token_here
```

## Publishing Steps

1. **Update the version**: Edit the version in `gradle.properties` or set it via command line

2. **Build and test**:

   ```bash
   ./gradlew clean build
   ```

3. **Publish to GitHub Packages**:

   ```bash
   ./gradlew publish
   ```

   Or with inline version:

   ```bash
   ./gradlew publish -Pversion=0.0.2
   ```

4. **Verify**: Check your GitHub repository's Packages tab to confirm the package was published

## Version Management

The project follows semantic versioning (MAJOR.MINOR.PATCH):

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

Update the version before each release:

```bash
./gradlew publish -Pversion=1.0.1
```

## Publishing Checklist

Before publishing a new version:

- [ ] All tests pass (`./gradlew test`)
- [ ] Code is properly formatted
- [ ] README is up to date
- [ ] Version number is updated
- [ ] Changelog is updated (if you create one)
- [ ] Git tag is created: `git tag v0.0.2 && git push origin v0.0.2`

## Consuming the Package

After publishing, users can consume the package by adding your repository to their build files:

### Gradle

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/mlanett/tpe-agent")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("mlanett:tpe-agent:0.0.2")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/mlanett/tpe-agent</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>mlanett</groupId>
        <artifactId>tpe-agent</artifactId>
        <version>0.0.2</version>
    </dependency>
</dependencies>
```

**Note**: Even for public packages, GitHub Packages requires authentication. Users will need their own Personal Access Token with `read:packages` scope.

## Troubleshooting

### Authentication Failed

- Verify your token has the correct scopes (`read:packages` or `write:packages`)
- Make sure the token hasn't expired
- Check that your username is correct

### Package Already Exists

- GitHub Packages doesn't allow overwriting existing versions
- You must increment the version number
- Delete the package from GitHub's UI if you need to republish the same version (not recommended)

### Wrong Repository URL

- The URL must match the GitHub repository you created
- Format: `https://maven.pkg.github.com/OWNER/REPOSITORY`
- Update `monitoring-agent/build.gradle.kts` if you used a different repository name

## Alternative: Using GitHub Actions

You can automate publishing with GitHub Actions. Create `.github/workflows/publish.yml`:

```yaml
name: Publish Package

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'

      - name: Validate Gradle wrapper
        uses: gradle/wrapper-validation-action@v1

      - name: Build and publish
        run: ./gradlew publish
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

This will automatically publish to GitHub Packages whenever you create a GitHub release.
