EXPORT PATH=${jre_install}/bin:$PATH
# shellcheck disable=SC2068
java -jar ${smssender_jar} $@