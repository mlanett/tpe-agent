# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0]

Major refactor. Extracted core classes as interfaces to solve classloader issues.

Renamed gradle modules.

## [0.1.0]

Minor API change: renamed ThreadPoolExecutorMetrics.getQueuedCount (was getQueueSize).

Updated the example code to use 0.1.0.

Updated docs.

Deleted unused code.

## [0.0.16]

Fixed publishing - this is the first release which published correctly.

## [0.0.1]

### Added

- Initial release of ThreadPoolExecutor Agent
- Automatic discovery and tracking of all ThreadPoolExecutor instances
- ByteBuddy-based bytecode instrumentation
- `ThreadPoolExecutorRegistry` for accessing tracked pools
- `ThreadPoolExecutorMetrics` for comprehensive pool metrics
- Support for both `-javaagent` and programmatic installation
- Weak references to prevent memory leaks
- Automatic pool naming based on ThreadFactory or class names

### Features

- Zero configuration required
- Tracks pools from application code and third-party libraries
- Thread-safe registry implementation
- Comprehensive metrics: queue size, active threads, completed tasks, pool sizes
- Java 11+ compatibility
- Apache License 2.0

[0.0.1]: https://github.com/mlanett/tpe-agent/releases/tag/v0.0.1
