# about this repo
This is a functional java code that shows how to perform an unzip of a non-trivial file

- it takes into account that a given zip file might have **sub-directories** and those are restored
- the **file permissions** of the files are restored (unix like OSs only)
- the **files might be symlinks** that needs to be recreated (unix like OSs only)
- makes **usage of parallelism** (at least 50% faster than 7zip ;) )

the code uses apache commons compress since it is the best way (in Java) to read and translate bytes from the extra fields where the file permissions are stored to later on translate them into unix permissions

# system requiremnts 
* Java 1.8 or higher
* Maven 3.5 or higher

# steps to create the jar
1. install java
2. install maven
3. clone project
4. execute ```mvn package```

# usage
```$  java -jar target/zipper.jar -h
-- HELP --
usage: java -jar zipper.jar -i /path/to/file.zip -o /path/to/dir
decompress
 -i,--input <arg>    Path to file that will be decompressed
 -o,--output <arg>   Output directory

usage: java -jar zipper.jar -c /path/to/file.zip -t /path/to/dir
compress
 -c,--create <arg>   Path to file that will be created
 -t,--target <arg>   Target file/directory that will be compressed

usage: java -jar zipper.jar -h
help
 -h,--help   Shows this message
 ```
 
# coming soon
* compression
* support for other formats such as tar/gz/7zip etc
