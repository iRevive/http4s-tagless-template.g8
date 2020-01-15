---
id: tests-execution
title: Tests execution
---

- [How to run tests](#how-to-run-tests)  
- [How to run integration tests](#how-to-run-integration-tests)  
- [How to calculate test coverage](#how-to-calculate-test-coverage)  

## <a name="how-to-run-tests"></a> How to run tests
In a `<root>` project directory write in a console  
```sh
\$ sbt test
```

## <a name="how-to-run-integration-tests"></a> How to run integration tests
**Note.** Installed docker is required for integration tests.

In a `<root>` project directory write in a console  
```sh
\$ sbt it:test
```

## <a name="how-to-calculate-test-coverage"></a> How to calculate coverage

In a `<root>` project directory write in a console  

```sh
\$ sbt clean coverage test it:test coverageReport
```
or 
```sh
\$ sbt testAll
```

Coverage reports will be in `target/scala-2.12/scoverage-report`. There are HTML and XML reports.  
