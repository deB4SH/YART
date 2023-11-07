# YART
YART, short term for Yet Another Random Templater, is a schema based templater written in Java designated for a wide range of possibilities.
It validates the given configuration yaml against the provided json schema and fills in all known values into the provided template.
The templating is done with [jinjava](https://github.com/HubSpot/jinjava), a jinja template engine for java. 

- Schema Validation
- Templating based on Schema
- Dynamic Folder Generation

---

# How It Works

1) YART is bundled as a jar-with-dependencies including everything you need to run it. 
Download it from the [package tab](https://github.com/deB4SH/YART/packages/1968663) and install Java 21
YART is also published as docker image also available under the [releases tab](https://github.com/deB4SH/YART/pkgs/container/yart).
2) Write your schema to validate any configuration against.
There are multiple examples included within the test resources. For example:
[Case: Simple Happy Path](https://github.com/deB4SH/YART/blob/main/src/test/resources/test_cases/02_simple_happy_path/schema/schema.json),
[Case: Subschema](https://github.com/deB4SH/YART/blob/main/src/test/resources/test_cases/04_subschema/schema/schema.json),
[Case: Complex Subschema](https://github.com/deB4SH/YART/blob/main/src/test/resources/test_cases/05_complex_subschema/schema/schema.json).
3) Write your templates that will be used.
There are also multiple examples included within the test resources. For example:
[Case: Simple Happy Path](https://github.com/deB4SH/YART/tree/main/src/test/resources/test_cases/02_simple_happy_path/template),
[Case: Subschema](https://github.com/deB4SH/YART/tree/main/src/test/resources/test_cases/04_subschema/template),
[Case: Complex Subschema](https://github.com/deB4SH/YART/tree/main/src/test/resources/test_cases/05_complex_subschema/template).
4) Create your config.yaml according to your schema. A valid config mal look like the one from [Case: Complex Subschema](https://github.com/deB4SH/YART/blob/main/src/test/resources/test_cases/05_complex_subschema/config/config.yaml)
5) Run the application with the required environment configuration
`java -jar /YOUR_DOWNLOAD_LOCATION/yart.jar --templatedirectory ${templatedirectory} --schemadirectory ${schemadirectory} --schemafilename ${schemafilename} --configfile ${configfile} --outputdirectory /YOUR_DESIRED_OUTPUT_LOCATION`

---

# Extendability

From time to time it is required to extend jinja a bit and therefore YART provides a `ExtensionProvider` to help you with embedding your own functionality.
It uses the underlying jinjava functionality of `ELFunctionDefinition` and allows you to add any functionality you desire. 

At date of writing YART provides by default three extensions.

| Extension Class | Namespace | Functionname   | Description                                                      |
|-----------------|-----------|----------------|------------------------------------------------------------------|
| Example         | example   | hello          | Provides a simple hello world message                            |
|                 |           |                |                                                                  |
| Terraform       | tf        | dictobjparser  | Parses a map object into a terraform adequat dict                |
| Terraform       | tf        | arrayctnstring | Parses an array of strings into a stringified array within jinja |

All currently available implementation are available under the package [de.b4sh.yart.extensions](https://github.com/deB4SH/YART/tree/main/src/main/java/de/b4sh/yart/extensions)

If you desire to build your own extension - Nothin is simpler then that.

1) Create your own package
2) Create your new extension and extend the class `ExtensionProvider`
3) Create a new static function with estimated parameters and a return as String 
4) Override the required abstract functions. Please be cautious for the value returned with the function `getFunctionName`. The value must resolve to your new function name
5) Implement your functionality within the function

The `ExtensionLoader` check before rendering which subclasses of the `ExtensionProvider` are available and loads them accordingly. 
You can follow the HelloWorld implementation [code wise](https://github.com/deB4SH/YART/blob/main/src/main/java/de/b4sh/yart/extensions/example/HelloWorld.java) and [test case wise](https://github.com/deB4SH/YART/tree/main/src/test/resources/test_cases/00_hello_world).

---

# Something Something

All this is possible to the awesome work of open source communities in various projects. For this some honorable mentions of used frameworks:

- [picocli](https://github.com/remkop/picocli)
- [jinjava](https://github.com/HubSpot/jinjava)
- [snakeyaml](https://github.com/snakeyaml/snakeyaml)
- [justify](https://github.com/leadpony/justify)
- [reflections](https://github.com/ronmamo/reflections)

Is something missing your desperately need? Something else?

Feel free to get in touch via mastodon: https://hachyderm.io/@deb4sh