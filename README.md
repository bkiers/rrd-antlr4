# RRD for ANTLR4

Railroad Diagrams for ANTLR v4 grammar rules.

## Introduction

This tool parses ANTLR 4 grammars and creates an html page containing
all parser- and lexer rules from this grammar with accompanying railroad
diagrams as well as creating separate railroad PNG images of all grammar
rules.

## Install

Clone this repository:

```
git clone https://github.com/bkiers/rrd-antlr4.git
```

Then build it:

```
cd rrd-antlr4
mvn clean package
```

The JAR file containing all dependencies can be found in the `target` folder.

To give it a test by parsing the JSON grammar found in the official ANTLR 4
repository, run the following command from the terminal:

```
cd target
java -jar rrd-antlr4-0.1.0.jar https://raw.github.com/antlr/grammars-v4/master/json/Json.g4
```

When the command above finishes, one html file and some png images
will have been generated in the folder `./output/Json`. The html file
does not make use of the png files but uses SVG to display the railroad
diagram and will look like this:

![json diagrams](TODO)

## Using programmatic

To use this library in your own code, either stick the JAR file in your
classpath, or add the following dependency to your POM:

```xml
<dependency>
    <groupId>nl.big-o</groupId>
    <artifactId>rrd-antlr4</artifactId>
    <version>0.1.0</version>
</dependency>
```

and parse a grammar file as follows:

```java
TODO
```

## Credits

* [ANTLR 4](https://github.com/antlr/antlr4) grammar (and runtime) to parse ANTLR 4 grammars
* [railroad-diagrams](https://github.com/tabatkins/railroad-diagrams) to create the SVG diagrams of ANTLR 4's rules
* [Batik](http://xmlgraphics.apache.org/batik/) to convert SVG to PNG

