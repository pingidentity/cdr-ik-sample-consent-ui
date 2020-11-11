printenv

cd /tmp

java -jar -Dserver.port=7879 -Dserver.baseurl=${BASE_URL} app.war run configure
