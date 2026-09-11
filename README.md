# CUBRID SQL Analyzer

**CUBRID SQL Analyzer** is a command-line tool for checking the compatibility of
Oracle database objects and SQL statements with CUBRID.

The analyzer reads DDL metadata from an Oracle database and DML statements from
XML mapper files when those sources are configured, validates them with the
embedded CUBRID parser or an online CUBRID target, and generates a report containing success and failure details. Failed
statements are also assigned an estimated migration cost based on configurable
rules.

## Key Features

* **Source Analysis**
  * Loads database objects from an Oracle JDBC connection when configured.
  * Extracts SQL statements from XML mapper files when configured.
  * Skips a missing or failed source and continues with the other source.
* **CUBRID Validation**
  * Validates statements with the embedded CUBRID parser.
  * Executes DDL and prepares DML without executing it against an online CUBRID JDBC target.
* **Execution Modes**
  * Interactive console mode.
  * Non-interactive command-line mode.
  * Terminal UI (TUI) mode.
* **Analysis Reports**
  * Summarizes analyzed, successful, and failed statements.
  * Records detailed failure information.
  * Estimates migration cost using rules defined in `settings/cost.json`.

## Project Structure

| Path | Description |
| --- | --- |
| `src/com/cubrid/sqlanalyzer/command` | CLI/TUI entry points, argument handling, services, and result views |
| `src/com/cubrid/sqlanalyzer/core` | Analysis configuration, execution plans, parsers, and cost calculation |
| `src/com/cubrid/sqlanalyzer/dmlparser` | XML mapper and DML statement parsing |
| `src/com/cubrid/sqlanalyzer/xmlmetadata` | XML source metadata loading |
| `src/dist` | Distribution scripts and default settings |
| `test` | Unit tests |
| `jni` | Native SQL validator library source and build files |
| `pl_server` | PL/CSQL parser dependency used during analysis |
| `submodule/cubrid-migration` | CUBRID Migration Toolkit modules used by the analyzer |

## Build from Source

### 1. Prerequisites

Install or prepare the following:

* Linux x86_64
* JDK 21 or later
* Apache Maven 3.9 or later
* Git
* GNU Make and GCC
* A CUBRID installation with the `CUBRID` environment variable configured
* `pl_server.jar` at `pl_server/pl_server.jar`

The build downloads and bundles the Linux x86_64 Eclipse Temurin OpenJDK 21
JRE. The machine running the packaged analyzer does not need a system Java
installation.

### 2. Clone Submodules

```bash
git submodule update --init --recursive
```

### 3. Build

Use `build.sh` as the single build entry point. For the initial build, include
`--with-submodules` to install the required CUBRID Migration Toolkit modules
into the project-local Maven repository:

```bash
bash build.sh --with-submodules
```

Subsequent builds reuse the installed CMT artifacts:

```bash
bash build.sh
```

Maven goals and options can be passed directly to the script:

```bash
bash build.sh clean package
```

| Option | Description |
| --- | --- |
| `-s`, `--with-submodules` | Builds and installs the required CMT submodules before SQL Analyzer |
| `-h`, `--help` | Displays the build script usage |

The default Maven arguments are:

```text
-DskipTests package
```

If you call Maven directly, keep using the project-local Maven repository:

```bash
mvn -Dmaven.repo.local=.build/submodule-m2repo clean compile
```

This repository is populated by the initial `bash build.sh --with-submodules`
run. On a fresh checkout, do not rely on the direct Maven command until the CMT
submodule artifacts have been installed there.

Use `clean` when switching between IDE builds and Maven builds. Some IDE
incremental compilers may leave broken classes in `target/classes`; a clean
compile forces Maven to regenerate them.

The Linux distribution archive is generated at:

```text
target/sql-analyzer-0.0.1-SNAPSHOT-linux-x86_64.tar.gz
```

The unpacked distribution files are available under `target/analyzer`.

### JDBC Drivers

The CUBRID JDBC driver is included in the analyzer distribution during the
build. No separate CUBRID JDBC driver setup is required for a normal build.

The Oracle JDBC driver (`ojdbc`) is not bundled. To analyze an Oracle source,
obtain the appropriate `ojdbc` JAR separately and place it in a JDBC driver
repository directory. Pass that directory with `-jr <directory>` or configure
it with `jdbc.repository.dir`.

## Running the Analyzer

Extract the distribution and run the launcher:

```bash
tar -xzf target/sql-analyzer-0.0.1-SNAPSHOT-linux-x86_64.tar.gz
cd sql-analyzer-0.0.1-SNAPSHOT
./analyzer.sh
```

The launcher always uses the bundled JRE under `jre`; it does not use the
system `java` command or `JAVA_HOME`.

Running without arguments starts the interactive console. The analyzer can also
be started with command-line options:

```bash
# Analyze SQL mapper XML files with the embedded parser
./analyzer.sh -sx -xd /path/to/sqlmap

# Start the terminal UI
./analyzer.sh -tui -sx -xd /path/to/sqlmap

# Analyze an Oracle database with the embedded parser
./analyzer.sh -so \
  -oj 'jdbc:oracle:thin:@//localhost:1521/xe|user|password'

# Analyze Oracle DDL and XML mapper DML together
./analyzer.sh -so \
  -oj 'jdbc:oracle:thin:@//localhost:1521/xe|user|password' \
  -sx -xd /path/to/sqlmap
```

For Oracle source analysis, configure the `ojdbc` driver as described in
[JDBC Drivers](#jdbc-drivers).

## Configuration

The default runtime configuration files are located in `settings`:

* `settings/setting.conf`: source, target, UI, JDBC, and logging options
* `settings/cost.json`: base costs and heuristic migration cost rules
* `settings/logback.xml`: logging configuration

Run the analyzer with a specific settings file:

```bash
./analyzer.sh -conf settings/setting.conf
```

### `setting.conf` Options

#### Common Options

| Property | Values / Default | Description |
| --- | --- | --- |
| `arguments` | Command-line arguments | Provides the complete analyzer argument string, for example `-so -oj jdbc\|user\|password -sx -xd ./sqlmap` |
| `ui.mode` | `console` or `tui` / `console` | Selects the user interface mode |
| `jdbc.repository.dir` | Directory path | Specifies a directory containing JDBC drivers |
| `log.dir` | Directory path / `logs` | Specifies the runtime log directory |

When `arguments` is set, it takes precedence over the structured source,
target, and UI properties below. Command-line analyzer options also take
precedence over options loaded from the settings file.

#### Source Options

| Property | Values / Default | Description |
| --- | --- | --- |
| `source.type` | `all`, `oracle`, or `xml` | Optional source hint. `all` means Oracle and XML may both be configured |
| `source.jdbc` | `<jdbcUrl\|user\|password>` | Provides an Oracle connection as a single value |
| `source.jdbc.url` | Oracle JDBC URL | Provides the Oracle JDBC URL separately |
| `source.host` | Host name | Oracle host used to construct the JDBC URL |
| `source.port` | Port number | Oracle port used with `source.host` and `source.sid` |
| `source.sid` | Service name or SID | Oracle service name used to construct the JDBC URL |
| `source.username` | User name | Oracle database user |
| `source.password` | Password | Oracle database password |
| `xml.directory` | Directory path | Directory containing XML mapper files |
| `xml.charset` | Charset / `UTF-8` | Character encoding of the XML files |

For an Oracle source, use either `source.jdbc`, `source.jdbc.url`, or the
`source.host`/`source.port`/`source.sid` combination. Oracle and XML settings
can be present together. If one source is missing or fails to load, the analyzer
records that source as skipped and continues with the other source. If both
sources fail, the analyzer exits.

#### Target Options

| Property | Values / Default | Description |
| --- | --- | --- |
| `target.type` | `parser`, `jdbc`, or `cubrid` | Selects embedded parser validation or an online CUBRID target; `cubrid` is an alias for `jdbc` |
| `target.jdbc` | `<jdbcUrl\|user\|password>` | Provides a CUBRID connection as a single value |
| `target.jdbc.url` | CUBRID JDBC URL | Provides the CUBRID JDBC URL separately |
| `target.username` | User name | CUBRID database user |
| `target.password` | Password | CUBRID database password |

### Configuration Examples

Analyze XML mapper files with the embedded parser:

```properties
ui.mode=console
xml.directory=./sqlmap
xml.charset=UTF-8
target.type=parser
```

Analyze against an online CUBRID target:

```properties
source.type=xml
xml.directory=./sqlmap
xml.charset=UTF-8

target.type=jdbc
target.jdbc.url=jdbc:cubrid:localhost:33000:demodb:::
target.username=dba
target.password=
```

Analyze Oracle DDL and XML mapper DML together:

```properties
ui.mode=tui
jdbc.repository.dir=./jdbc

source.type=all
source.host=localhost
source.port=1521
source.sid=xe
source.username=oracle_user
source.password=oracle_password

xml.directory=./sqlmap
xml.charset=UTF-8

target.type=parser
```

> **Security Note:** Database passwords in `setting.conf` are stored as plain
> text. Restrict access to the file and do not commit credentials to source
> control.

## Output

Analysis results are written to the `report` directory as UTF-8 text files:

```text
report/analyzer_result_<timestamp>.txt
```

Runtime logs are written to the `logs` directory by default.

## Tests

Run the unit test suite with the same build entry point:

```bash
bash build.sh test
```

Add `--with-submodules` when the CMT artifacts have not been installed yet or
when the submodule source has changed.

### Testability Notes

Several command-layer classes expose package-private constructors or small
interfaces so integration-style tests can drive the full flow without touching
external systems:

* `AnalyzerConsoleRunner` keeps its public constructor for production use, and
  also accepts an injected `AnalyzerService` in tests. This lets tests verify
  console flow, exit codes, and error handling without forcing a real service
  implementation.
* `AnalyzerExecutionRunner` creates JDBC executors through
  `AnalyzerJdbcExecutorFactory`. Production code opens a real JDBC connection
  from `AnalyzerConfiguration`, while tests can inject `ScriptedJdbcExecutor`
  to record executed SQL, commits, cleanup order, and failures without a
  database.
* `AnalyzerReportWriter` keeps writing to `report` by default, and also accepts
  an injected report directory. Tests use a temporary directory so report files
  do not pollute the project workspace.

These seams are intentionally package-private and small. They preserve normal
runtime behavior while making end-to-end command scenarios deterministic in
JUnit.
