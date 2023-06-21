#!/bin/sh
cd "${INSTALL_PATH}" || exit

if [ $# -eq 0 ]; then
    # shellcheck disable=SC2154
    "${jre_install}"/bin/java -jar "smssender.jar"
else
    "${jre_install}"/bin/java -jar "smssender.jar" "$@"
fi
exit 0