#!/bin/sh
exec gradle -p "$(dirname "$0")" "$@"
