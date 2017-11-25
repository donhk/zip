# about this repo
This is a functional java code that shows how to perform an unzip of a non-trivial file

- it takes into account that a given zip file might have **sub-directories** and those are restored
- the **file permissions** of the files are restored (unix like OSs only)
- the **files might be symlinks** that needs to be recreated (unix like OSs only)
- makes **usage of parallelism** (at least 50% faster than 7zip ;) )

the code uses apache commons compress since it is the best way (in Java) to read and translate bytes from the extra fields where the file permissions are stored to later on translate them into unix permissions

# system requiremnts 
* Java 1.8 or higher
* Maven 3.5

# steps to create the jar
1. install java
2. install maven
3. clone project
4. execute ```mvn package```

# usage
```java -jar zipper.jar /path/to/file.zip /path/to/target/directory```
