#!/bin/bash

error_exit ()
{
    echo "$1"
    exit 1
}

[ -f java/bin/st.jar ] || error_exit "bad start directory"

java -classpath java/bin/st.jar org.kbsriram.multisign.CMain kbs.gpg.txt $HOME/.gnupg/kbsriram.com.skr.asc .

# crosscheck with gpg
for i in `find . -type f -name '*.html' -print`
do
    gpg --status-fd 1 --verify ${i}.asc 2>/dev/null | grep 'GOODSIG 62F463C673F6C01F KB Sriram' > /dev/null || error_exit "$i has bad signature"
done
