#!/bin/bash

error_exit ()
{
    echo "$1"
    exit 1
}

[ -f java/bin/st.jar ] || error_exit "bad start directory"

find . -name media -prune -o -type f -name '*.html' -exec rm -f '{}' \;
java -jar java/bin/st.jar tpl txt . || error_exit "java failed"
find . -name media -prune -o -type f -name '*.html' -print | xargs -n1 $HOME/extbuilds/bin/tidy -eq || error_exit "Bad html"
