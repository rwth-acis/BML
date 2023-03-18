# BML
[![Unit Tests](https://github.com/rwth-acis/BML/actions/workflows/unit-tests.yml/badge.svg)](https://github.com/rwth-acis/BML/actions/workflows/unit-tests.yml)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=rwth-acis_BML&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=rwth-acis_BML)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=rwth-acis_BML&metric=bugs)](https://sonarcloud.io/summary/new_code?id=rwth-acis_BML)

Parser, language server and transpiler for a unified formal bot modeling language. (Work in progress)

## Project structure 
- `:parser` (contains ANTLR grammar [BML.g4](https://github.com/rwth-acis/BML/blob/main/parser/src/main/antlr/BML.g4) and semantic analysis)
- `:transpiler` (contains code generation) <b>DEPENDS ON</b> `:parser`
- `:lang-server` (contains implementation of [LSP](https://microsoft.github.io/language-server-protocol/)) <b>DEPENDS ON</b> `:parser`

## Documentation
Auto-generated Javadoc can be found [todo]().

## Requirements
- Java 17

## Build
To make a full build (i.e., run tests, etc.):
```bash
gradle clean transpiler:build
```

To only build the jar:
```bash
gradle clean transpiler:executableJar
```

## Test
Currently only the parser has tests, which can be run as follows:
```bash
gradle parser:clean parser:test
```

## Run bmlc
```bash
usage: bmlc
 -f,--format <jar|java>         format of the output, either jar or java
 -i,--input <path>              define input BML file, path can be
                                relative to executable or absolute
 -o,--output <path>             define output directory
 -p,--package <package-names>   define your package name, e.g.,
                                com.example.project
```

## Contributing
Please feel free to contribute to this project.  
Should you find a problem/crash/etc. that you simply want to report, feel free to open an issue.

1. Fork the project
2. Create your feature branch (`git checkout -b feat-<feature-name>`)
3. Implement
4. Add your files to the staging area (`git add <your-files>`)
5. Commit your changes (`git commit -m '<Parser|Transpiler|Lang-Server>: describe what you did!'`)
6. Push the branch (`git push origin feat-<feature-name>`)
7. Create a new [PR](https://github.com/rwth-acis/BML/pulls)
