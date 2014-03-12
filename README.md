# RRD for ANTLR4

Railroad Diagrams for ANTLR 4 grammar rules.

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

![json diagrams](https://raw.github.com/bkiers/rrd-antlr4/master/static/json.png)

The parameter can be a remove file (as shown in the example above), or
a local file but can even be a grammar. The command below:

```
java -jar target/rrd-antlr4-0.1.0.jar "grammar T; parse : .* EOF; ANY : . ;"
```

will generate the following 2 diagrams:

![parse diagram](https://raw.github.com/bkiers/rrd-antlr4/master/static/parse.png)

![ANY diagram](https://raw.github.com/bkiers/rrd-antlr4/master/static/ANY.png)

## Using programmatically

To use this library in your own code, either stick the JAR file
(rrd-antlr4-0.1.0.jar) in your classpath, or add the following
dependency to your POM (after having done a `mvn clean package`!):

```xml
<dependency>
    <groupId>nl.big-o</groupId>
    <artifactId>rrd-antlr4</artifactId>
    <version>0.1.0</version>
</dependency>
```

and parse a grammar file as follows:

```java
String grammarFileName = "/path/to/Json.g4";
DiagramGenerator generator = new DiagramGenerator(grammarFileName);

// Print all parsed rules
System.out.println("all parsed rules: " + generator.getRules().keySet());

// The name of the rule to create a railroad diagram of
String ruleName = "jsonObject";

// Get the SVG of the rule
String svg = generator.getSVG(ruleName);
System.out.println("the svg looks like this: " + svg);

// Create the PNG railroad diagram
boolean success = generator.createDiagram(ruleName);
System.out.println("successfully created diagram: " + success);

// Create an html file containing all rules
success = generator.createHtml("/path/to/file.html");
System.out.println("successfully created the html file: " + success);
```

## Credits

* [railroad-diagrams](https://github.com/tabatkins/railroad-diagrams) to create the SVG diagrams of ANTLR 4's rules
* [ANTLR 4](https://github.com/antlr/antlr4) grammar (and runtime) to parse ANTLR 4 grammars
* [Batik](http://xmlgraphics.apache.org/batik) to convert SVG to PNG

## License

[The MIT License (MIT)](http://opensource.org/licenses/MIT)

```
Copyright (c) 2014 Bart Kiers

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
