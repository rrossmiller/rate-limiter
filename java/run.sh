# javac *.java &&
# java App $@
mvn clean install package && 
	clear &&
	echo '****************' &&
	# java -cp target/ratelimiter-1.jar com.rate.App $@
	# mvn exec:java -Dexec.mainClass="com.rate.App" -Dexec.args="$@"
	java -jar target/ratelimiter-1.jar $@
