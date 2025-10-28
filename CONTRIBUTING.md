# Contributing to ThreadPoolExecutor Agent

Thank you for your interest in contributing to tpe-agent! This document provides guidelines and instructions for contributing.

## Code of Conduct

This project aims to be welcoming and respectful to all contributors. Please be kind and considerate in all interactions.

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue on GitHub with:

- A clear, descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Your Java version and operating system
- Relevant logs or stack traces (if applicable)

### Suggesting Enhancements

Enhancement suggestions are welcome! Please create an issue with:

- A clear description of the enhancement
- The use case or problem it solves
- Any implementation ideas (optional)

### Pull Requests

1. **Fork the repository** and create a new branch from `main`

   ```bash
   git checkout -b feature/my-new-feature
   ```

2. **Make your changes** with clear, focused commits
   - Follow the existing code style
   - Keep commits atomic and well-described
   - Write clear commit messages

3. **Test your changes**

   ```bash
   ./gradlew clean build test
   ```

4. **Update documentation** if needed
   - Update README.md for user-facing changes
   - Update code comments and JavaDoc
   - Add entries to CHANGELOG.md

5. **Submit a pull request** with:
   - A clear description of the changes
   - Reference to any related issues
   - Screenshots or examples (if applicable)

## Development Setup

### Prerequisites

- JDK 11 or later
- Git

### Building from Source

```bash
# Clone the repository
git clone https://github.com/mlanett/tpe-agent.git
cd tpe-agent

# Build the project
./gradlew build

# Run tests
./gradlew test

# Install to local Maven repository for testing
./gradlew publishToMavenLocal
```

### Project Structure

```
tpe-agent/
├── monitoring-agent/           # Main agent module
│   └── src/main/java/
│       └── mlanett/tpe_agent/
│           ├── ThreadPoolExecutorAgent.java      # Main agent entry point
│           ├── ThreadPoolExecutorRegistry.java   # Registry of tracked pools
│           ├── ThreadPoolExecutorMetrics.java    # Metrics data class
│           ├── ThreadPoolExecutor*Advice.java    # ByteBuddy advice classes
│           └── Logger.java                       # Simple logging utility
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Gradle settings
└── README.md                   # Documentation
```

## Code Style

- Follow standard Java naming conventions
- Use 4 spaces for indentation (no tabs)
- Keep lines under 120 characters when practical
- Write JavaDoc for public APIs
- Use meaningful variable and method names

## Testing

Currently, the project doesn't have automated tests. Contributions adding test coverage are highly welcome!

If you're adding tests, please:

- Use JUnit 5
- Test both success and failure scenarios
- Mock external dependencies when appropriate

## Documentation

- Keep README.md up to date
- Document all public APIs with JavaDoc
- Update CHANGELOG.md for all user-facing changes
- Include code examples for new features

## Releasing

Only project maintainers can publish releases. The process is:

1. Update version in `gradle.properties`
2. Update CHANGELOG.md with release date
3. Commit and tag the release:

   ```bash
   git tag -a v0.0.2 -m "Release version 0.0.2"
   git push origin v0.0.2
   ```

4. Publish to GitHub Packages:

   ```bash
   ./gradlew publish
   ```

5. Create a GitHub Release with notes from CHANGELOG

## Questions?

If you have questions about contributing, feel free to:

- Open an issue for discussion
- Reach out to the maintainers

Thank you for contributing! 🎉
