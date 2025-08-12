# Changelog

## 0.1.0 - 2025-08-12

### Forked from [`xerial/sqlite-jdbc`](https://github.com/xerial/sqlite-jdbc) 3.50.2.0

### Added

- FFM interface to SQLite native library

### Changed

- JDK requirement: 11 → 22
- Replace raw `long` pointers with FFM `MemorySegment`s
- Replace Apache FastDateFormat uses with `java.time.format.DateTimeFormatter` and other built-ins
- Numerous other small internal changes

### Removed

- Embedded SQLite native library
  - C build steps
  - JNI bindings
  - SQLite extension functions
  - Extraction/loading utilities (`org.sqlite.util.*`, `org.sqlite.SQLiteJDBCLoader`)
- Embedded Apache FastDateFormat (`org.sqlite.date.*`)
- GraalVM NativeImage support
