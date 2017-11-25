# about
This is a functional code that shows how to perform an unzip with java of a non trivial zip file, the takes into account
that a given zip file will have sub-directories and those need to be restored, the files permissions of the files needs to be restored,
the files might be symlinks that needs to be recreated and for large files the usage of parallelism helps a lot.

the code makes use of apache commons compress since is the best way to read the extra fields where the file permissions are stores in the zip files and translate the bytes into unix permissions

# zip
java unzip/zip class using apache commons compress

# system requiremnts 
* Java 1.8 or higher
* Maven 3.5

# steps to create the jar
1. install java
2. install maven
3. clone project
4. execute mvn package

# usage
```java -jar zipper.jar /path/to/file.zip /path/to/target/directory```
