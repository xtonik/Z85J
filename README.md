# Z85J

Pure Java implemtentation of encoding Z85 defined in ZeroMQ's RFC32.

## Features
**- enconder / decoder / validator** - all of them accept as its input byte[], decoder and validator accept String also
- prefer validator above decoder as it is more performant
- processing byte[] inputs is faster than processing String as a input
- it requires size being multiple of block sizes - multiple of 5 for decoding , multiple of 4 for encoding
- it detects all invalid characters during decoding
- it detects overflow of block during decoding - highest valid decoded value `85**5 - 1` is greater than `2**32 - 1` and thus it could not fit into integer value range and it is considered as an invalid
- it is around 10 times faster than corresponding implementation in [jeromq](https://github.com/zeromq/jeromq/blob/f540268c81d787aee5f5ec9bc74a937a7f1ee8e8/jeromq-core/src/main/java/zmq/util/Z85.java).
  
## Requirements
- Java 8+
- Maven 3.x
- JUnit 5.x

## Installation
Clone the repository and build using Maven:

```bash
git clone https://github.com/xtonik/Z85J.git
cd xtonik
mvn clean install

java -jar target/Z85J-1.0-SNAPSHOT.jar
```

## Examples of Use

```java
byte[] encoded = Z85.encode(new byte[]{-1, -1, -1, -1});

byte[] decoded = Z85.decode("01234");
byte[] decoded = Z85.decode(new byte[]{'0','1','2','3','4'});

boolean valid = Z85.isValid("01234");
boolean valid = Z85.isValid(new byte[]{'0','1','2','3','4'});
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License. You are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of this software, as long as you include the original copyright and license notice in all copies or substantial portions of the software.

## TODO
- [ ] Rewrite benchmark to use JMH and move it to separate module if necessary
- [ ] Register project in Maven central repository
- [ ] Configure Checkstyle, FindBugs
- [ ] Implement padding support
- [ ] Reduce test flakiness
- [ ] Try to optimize decoder using: mapping by two characters at once, Unsafe.putLong(), simple expression instead of Horner's schema
- [ ] Try to optimize encoder using: Unsafe.getLong(), Unsafe.putInt(),
- [ ] Add support for more input / output types - e.g. InputStream, ByteBuffer and other types
- [ ] Compare performance and validity with third-party implementations, primarily written in Java, inform project owners about the bugs
