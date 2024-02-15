clear
echo '****************'
javac *.java &&
	java App $@ | less
